package com.ovelin.mall.auth.infrastructure.persistence.gateway;

import com.ovelin.mall.auth.domain.exception.InvalidIdentifierStatusException;
import com.ovelin.mall.auth.domain.exception.IdentifierAlreadyTakenException;
import com.ovelin.mall.auth.domain.exception.IdentifierNotExistException;
import com.ovelin.mall.auth.domain.exception.IdentifierNotOwnedByUserException;
import com.ovelin.mall.auth.domain.model.enums.IdentityLookupStatus;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifier;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifierFactory;
import com.ovelin.mall.auth.infrastructure.persistence.mapper.IdentityLookupMapper;
import com.ovelin.mall.auth.infrastructure.persistence.po.IdentityLookupPO;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * identity_lookup 的访问网关（infrastructure 内部，不对 domain / application 暴露）。
 * <p>
 * lookup 按 (identity_type, identifier) 哈希分片，与聚合（按 userId 分片）通常不在同一个库
 * <ol>
 *   <li>{@link #reserve}：INSERT PENDING，主键冲突即标识被占用</li>
 *   <li>聚合落库成功后 {@link #confirm}；失败则 {@link #cancelReserve}</li>
 *   <li>进程崩溃遗留的 PENDING 由 {@link #reclaimReservedStale} 定时回收</li>
 * </ol>
 * reserve / confirm / release 各自独立提交（REQUIRES_NEW），不会被外层事务回滚带走，
 * 也不会被外层事务长时间持有行锁。
 */
@Component
@RequiredArgsConstructor
public class IdentityLookupGateway {

    private final IdentityLookupMapper mapper;

    /** 待回收的占位记录（identityType 为原始 code，由调用方自行转换） */
    public record PendingLookup(IdentityIdentifier identifier, ShardedId userId) {
    }
    //  ------------------------------------------------------------------  reserve


    /**
     * 占位。同一用户对同一标识重复占位（重试）视为幂等成功。
     * 注意，用户注册时重试和网路抖动都会产生不同的ID，除非在第一次请求时记录下用户的id
     *
     * @throws IdentifierAlreadyTakenException 标识已被其他账号占用，自己抢占失败，但查不出这个身份，重新插入该身份又失败，发生极端并发
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reserve(IdentityIdentifier identifier, ShardedId userId) {
        idempotentInsert(identifier, userId, IdentityLookupStatus.RESERVED);
    }


    /**
     * 确认：聚合落库成功后调用，PENDING → ACTIVE。已是本用户的 ACTIVE 视为幂等成功。
     *
     * @throws IllegalStateException 占位已丢失（例如落库太慢被回收任务清理），调用方需让本次操作失败
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void confirm(IdentityIdentifier identifier, ShardedId userId) {
        idempotentUpdateStatus(identifier, userId,  IdentityLookupStatus.RESERVED, IdentityLookupStatus.ACTIVE);
    }

    /**
     * 同分片场景：在调用方事务内直接写入"已确认"的标识，随外层事务一起提交/回滚。
     * 相当于 reserve + confirm 合并为一步，不产生 PENDING 记录。
     * 调用方必须保证 lookup 与聚合路由到同一个库。
     *
     * @throws IdentifierAlreadyTakenException 标识已被占用
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void insertConfirmed(IdentityIdentifier identifier, ShardedId userId) {
        IdentityLookupPO po = newLookup(identifier, userId, IdentityLookupStatus.ACTIVE);
        if (tryInsert(po)) {
            return;
        }
        throw new IdentifierAlreadyTakenException(identifier);
    }
    /** 取消抢占，用于抢占后其它事务失败回滚；或cancelReserve失败后的自动任务重试*/
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelReserve(IdentityIdentifier identifier, ShardedId userId) {
        idempotentDelete(identifier, userId, IdentityLookupStatus.RESERVED);
    }



    //   ------------------------------------------------------------------    release
    /**
     * 申请释放
     * @param identifier
     * @param userId
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void beginRelease(IdentityIdentifier identifier, ShardedId userId){
        idempotentUpdateStatus(identifier, userId, IdentityLookupStatus.ACTIVE, IdentityLookupStatus.RELEASING);
    }
    /** 释放：补偿 / 解绑 / 注销时调用。只删除属于该用户的记录，不存在也不报错 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeRelease(IdentityIdentifier identifier, ShardedId userId) {
        idempotentDelete(identifier, userId, IdentityLookupStatus.RELEASING);
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void abortRelease(IdentityIdentifier identifier, ShardedId userId){
        idempotentUpdateStatus(identifier, userId, IdentityLookupStatus.RELEASING, IdentityLookupStatus.ACTIVE);
    }

    /** 登录读路径：标识 → userId，只认已确认的记录 */
    public Optional<ShardedId> findUserId(IdentityIdentifier identifier) {
        IdentityLookupPO po = mapper.selectActive(identifier.type().getCode(), identifier.value());
        return Optional.ofNullable(po).map(p -> ShardedId.fromId(p.getUserId()));
    }

    /**
     * 回收崩溃遗留的 PENDING（供定时任务调用）。
     * 对创建时间早于 olderThan 的 PENDING，交给 identityExists 判断身份行是否已落库：
     * 已落库 → 补确认（ACTIVE）；未落库 → 删除占位。两种处理都只作用于仍为 PENDING 的行。
     * 查询不带分片键，会广播到所有分片，仅用于后台任务。
     *
     * @return 本次处理的记录数
     */
    public int reclaimReservedStale(Duration olderThan, int limit, Predicate<PendingLookup> identityExists) {
        List<IdentityLookupPO> stale = mapper.selectStale(IdentityLookupStatus.RESERVED, olderThan.getSeconds(), limit);
        int handled = 0;
        for (IdentityLookupPO po : stale) {
            IdentityIdentifier identifier = IdentityIdentifierFactory.reconstitute(po.getIdentityType(), po.getIdentifier());
            ShardedId userId = ShardedId.fromId(po.getUserId());
            PendingLookup pending = new PendingLookup(identifier, userId);

            if (identityExists.test(pending)) {
                tryTransition(identifier, userId, IdentityLookupStatus.RESERVED, IdentityLookupStatus.ACTIVE);
            } else {
                tryDeleteIfMatch(identifier, userId, IdentityLookupStatus.RESERVED);
            }
            handled++;
        }
        return handled;
    }

    /**
     * 回收崩溃遗留的 RESERVED（供定时任务调用）。
     * 对创建时间早于 olderThan 的 Releasing，交给 identityExists 判断身份行是否已落库：
     * 已落库 → 补确认（ACTIVE）；未落库 → 删除占位。两种处理都只作用于仍为 RESERVED 的行。
     * 查询不带分片键，会广播到所有分片，仅用于后台任务。
     *
     * @return 本次处理的记录数
     */
    public int reclaimReleasingStale(Duration olderThan, int limit, Predicate<PendingLookup> identityExists) {
        List<IdentityLookupPO> stale = mapper.selectStale(IdentityLookupStatus.RELEASING, olderThan.getSeconds(), limit);
        int handled = 0;
        for (IdentityLookupPO po : stale) {
            IdentityIdentifier identifier = IdentityIdentifierFactory.reconstitute(po.getIdentityType(), po.getIdentifier());
            ShardedId userId = ShardedId.fromId(po.getUserId());
            PendingLookup pending = new PendingLookup(identifier, userId);

            if (identityExists.test(pending)) {
                tryTransition(identifier, userId, IdentityLookupStatus.RELEASING, IdentityLookupStatus.ACTIVE);
            } else {
                tryDeleteIfMatch(identifier, userId, IdentityLookupStatus.RELEASING);
            }
            handled++;
        }
        return handled;
    }

    // -------------------------------- 内部，幂等操作
    private void idempotentInsert(IdentityIdentifier identifier, ShardedId userId, IdentityLookupStatus status){
        IdentityLookupPO po = newLookup(identifier, userId, status);
        if (tryInsert(po)) {
            return;
        }
        IdentityLookupPO existing = selectByIdentifier(identifier);
        if (existing == null) {
            // 竞态：冲突方刚好被释放，重试一次
            if (tryInsert(po)) {
                return;
            }
            throw new IdentifierAlreadyTakenException(identifier);
        }
        requireExisting(identifier,  userId, status,existing);
    }

    private void idempotentUpdateStatus(IdentityIdentifier identifier, ShardedId userId, IdentityLookupStatus from, IdentityLookupStatus to){
        if (tryTransition(identifier, userId, from, to)){
            // 直接成功返回
            return;
        }
        IdentityLookupPO existing = selectByIdentifier(identifier);

        if (existing == null){
            throw new IdentifierNotExistException(identifier);
        }
        requireExisting(identifier, userId, to, existing);
        // 要修改的身份属于当前用户，且已经变成想要的状态
    }

    private void idempotentDelete(IdentityIdentifier identifier, ShardedId userId, IdentityLookupStatus status){
        if (tryDeleteIfMatch(identifier, userId, status)){
            return;
        }
        IdentityLookupPO existing = selectByIdentifier(identifier);
        if (existing == null){
            // 其实已删除
            return;
        }
        requireExisting(identifier,  userId, status,existing);
        // 属主和状态都匹配却没删掉：说明读写之间被并发修改
        throw new ConcurrentModificationException("identity_lookup 并发修改，请重试");
    }

    private void requireExisting(IdentityIdentifier identifier,  ShardedId userId, IdentityLookupStatus status,IdentityLookupPO existing){
        if (!Objects.equals(existing.getUserId(), userId.value())){
            throw new IdentifierNotOwnedByUserException(identifier, userId, existing.getUserId());
        }
        if (existing.getStatus() != status.getCode()){
            throw new InvalidIdentifierStatusException(identifier, IdentityLookupStatus.fromCode(existing.getStatus()));
        }
    }

    // ------------------------------------------------------------------ 内部，简单操作

    private IdentityLookupPO newLookup(IdentityIdentifier identifier, ShardedId userId, IdentityLookupStatus status){
        IdentityLookupPO po = new IdentityLookupPO();
        po.setIdentityType(identifier.type().getCode());
        po.setIdentifier(identifier.value());
        po.setUserId(userId.value());
        po.setStatus(status.getCode());
        return  po;
    }

    private IdentityLookupPO selectByIdentifier(IdentityIdentifier identifier){
        return mapper.selectByIdentifier(identifier.type().getCode(), identifier.value());
    }
    private boolean tryInsert(IdentityLookupPO po) {
        try {
            mapper.insert(po);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }


    private boolean tryTransition(IdentityIdentifier identifier, ShardedId userId, IdentityLookupStatus from, IdentityLookupStatus to){
        return mapper.transition(identifier.type().getCode(), identifier.value(), userId.value(), from, to) == 1;
    }

    private boolean tryDeleteIfMatch(IdentityIdentifier identifier, ShardedId userId, IdentityLookupStatus status){
        return mapper.deleteIfMatch(identifier.type().getCode(), identifier.value(), userId.value(), status) == 1;
    }

}