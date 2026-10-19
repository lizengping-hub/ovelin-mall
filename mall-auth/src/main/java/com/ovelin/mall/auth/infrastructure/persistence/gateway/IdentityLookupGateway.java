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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * lookup 按 (identity_type, identifier) 哈希分片，与聚合（按 userId 分片）通常不在同一个库。
 * 状态机：
 * <pre>
 *   (无) --reserve--> RESERVED --confirm--> ACTIVE --beginRelease--> RELEASING --completeRelease--> (删除)
 *                        |                     ^                         |
 *                  cancelReserve               +-------abortRelease------+
 *                        v
 *                     (删除)
 * </pre>
 * <ol>
 *   <li>{@link #reserve}：INSERT RESERVED，主键冲突即标识被占用</li>
 *   <li>聚合落库成功后 {@link #confirm}；失败则 {@link #cancelReserve}</li>
 *   <li>进程崩溃遗留的 RESERVED 由 {@link #reclaimReservedStale} 定时回收，
 *       遗留的 RELEASING 由 {@link #reclaimReleasingStale} 定时回收</li>
 * </ol>
 * reserve / confirm / cancelReserve / beginRelease / completeRelease / abortRelease 各自独立提交（REQUIRES_NEW），
 * 不会被外层事务回滚带走，也不会被外层事务长时间持有行锁。
 * 只有 {@link #insertConfirmed} 要求加入调用方事务（MANDATORY）。
 * <p>
 * 除 {@link #findUserId} 外，所有状态变更方法都是幂等的：
 * 目标状态已达成且属于同一用户时直接成功；记录已属于其他用户时视为已完成（补偿目标已达成），直接返回
 * 状态不符合抛 {@link InvalidIdentifierStatusException}。
 */
@Component
@RequiredArgsConstructor
public class IdentityLookupGateway {
    private static final Logger log = LoggerFactory.getLogger(IdentityLookupGateway.class);
    private final IdentityLookupMapper mapper;

    /** 待回收的占位记录（身份标识 + 持有者 userId，供回收任务判断身份行是否已落库） */
    public record PendingLookup(IdentityIdentifier identifier, ShardedId userId) {
    }

    //  ------------------------------------------------------------------  reserve

    /**
     * 占位：INSERT RESERVED。同一用户对同一标识重复占位（重试）视为幂等成功。
     * 注意：用户注册时的重试和网络抖动都会产生不同的 userId，除非在第一次请求时就记录下 userId，
     * 否则重试会被当作"另一个用户"而被拒绝。
     *
     * @throws IdentifierNotOwnedByUserException 标识已被其他用户占用（任意状态）
     * @throws InvalidIdentifierStatusException  标识已是本用户的，但状态不是 RESERVED（已 ACTIVE / RELEASING）
     * @throws IdentifierAlreadyTakenException  upsert 后查不到记录（理论上不会发生）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reserve(IdentityIdentifier identifier, ShardedId userId) {
        idempotentInsert(identifier, userId, IdentityLookupStatus.RESERVED);
    }

    /**
     * 确认：聚合落库成功后调用，RESERVED → ACTIVE。已是本用户的 ACTIVE 视为幂等成功。
     *
     * @throws IdentifierNotExistException       占位已丢失（例如落库太慢被回收任务清理），调用方需让本次操作失败
     * @throws IdentifierNotOwnedByUserException 记录属于其他用户
     * @throws InvalidIdentifierStatusException  记录处于 RELEASING
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void confirm(IdentityIdentifier identifier, ShardedId userId) {
        idempotentUpdateStatus(identifier, userId, IdentityLookupStatus.RESERVED, IdentityLookupStatus.ACTIVE);
    }

    /**
     * 同分片场景：在调用方事务内直接写入"已确认"（ACTIVE）的标识，随外层事务一起提交/回滚。
     * 相当于 reserve + confirm 合并为一步，不产生 RESERVED 记录。
     * 调用方必须保证 lookup 与聚合路由到同一个库。非幂等：重复插入同一标识会失败。
     *
     * @throws IdentifierAlreadyTakenException 标识已被占用（无论属于谁、处于什么状态）
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void insertConfirmed(IdentityIdentifier identifier, ShardedId userId) {
        IdentityLookupPO po = newLookup(identifier, userId, IdentityLookupStatus.ACTIVE);
        if (tryInsert(po)) {
            return;
        }
        throw new IdentifierAlreadyTakenException(identifier);
    }

    /**
     * 取消占位：删除本用户的 RESERVED 记录。用于占位后其它事务失败回滚，
     * 或 cancelReserve 自身失败后由自动任务重试。记录不存在视为幂等成功。
     *
     * @throws IdentifierNotOwnedByUserException 记录属于其他用户
     * @throws InvalidIdentifierStatusException  记录已 ACTIVE / RELEASING，不能当作占位取消
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelReserve(IdentityIdentifier identifier, ShardedId userId) {
        idempotentDelete(identifier, userId, IdentityLookupStatus.RESERVED);
    }

    //   ------------------------------------------------------------------    release

    /**
     * 申请释放：ACTIVE → RELEASING（登录路由立即不可见）。已是本用户的 RELEASING 视为幂等成功。
     *
     * @throws IdentifierNotExistException       记录不存在
     * @throws IdentifierNotOwnedByUserException 记录属于其他用户
     * @throws InvalidIdentifierStatusException  记录仍是 RESERVED（尚未确认）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void beginRelease(IdentityIdentifier identifier, ShardedId userId) {
        idempotentUpdateStatus(identifier, userId, IdentityLookupStatus.ACTIVE, IdentityLookupStatus.RELEASING);

    }

    /**
     * 完成释放：删除本用户的 RELEASING 记录。记录不存在视为幂等成功。
     *
     * 记录已属于其他用户时视为已完成（补偿目标已达成），直接返回
     * @throws InvalidIdentifierStatusException  记录不处于 RELEASING（必须先 {@link #beginRelease}）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeRelease(IdentityIdentifier identifier, ShardedId userId) {
        idempotentDelete(identifier, userId, IdentityLookupStatus.RELEASING);
    }

    /**
     * 放弃释放（补偿）：RELEASING → ACTIVE，标识重新对登录路由可见。已是本用户的 ACTIVE 视为幂等成功。
     *
     * @throws IdentifierNotExistException       记录不存在
     * @throws IdentifierNotOwnedByUserException 记录属于其他用户
     * @throws InvalidIdentifierStatusException  记录仍是 RESERVED
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void abortRelease(IdentityIdentifier identifier, ShardedId userId) {
        idempotentUpdateStatus(identifier, userId, IdentityLookupStatus.RELEASING, IdentityLookupStatus.ACTIVE);
    }

    /** 登录读路径：标识 → userId，只认已确认（ACTIVE）的记录 */
    public Optional<ShardedId> findUserId(IdentityIdentifier identifier) {
        IdentityLookupPO po = mapper.selectActive(identifier.type().getCode(), identifier.value());
        return Optional.ofNullable(po).map(p -> ShardedId.fromId(p.getUserId()));
    }

    /**
     * 回收崩溃遗留的 RESERVED（供定时任务调用）。
     * 对 updated_at 早于 olderThan 的 RESERVED，交给 identityExists 判断身份行是否已落库：
     * 已落库 → 补确认（ACTIVE）；未落库 → 删除占位。两种处理都只作用于仍为 RESERVED 的行。
     * 查询不带分片键，会广播到所有分片，仅用于后台任务。
     *
     * @return 实际还原或删除的记录数
     */
    public int reclaimReservedStale(Duration olderThan, int limit, Predicate<PendingLookup> identityExists) {
        List<IdentityLookupPO> stale = mapper.selectStale(IdentityLookupStatus.RESERVED.getCode(), olderThan.getSeconds(), limit);
        log.debug("Found reserved stale {}", stale.size());
        int handled = 0;
        for (IdentityLookupPO po : stale) {
            try {
                IdentityIdentifier identifier = IdentityIdentifierFactory.reconstitute(po.getIdentityType(), po.getIdentifier());
                ShardedId userId = ShardedId.fromId(po.getUserId());
                PendingLookup pending = new PendingLookup(identifier, userId);

                if (identityExists.test(pending)) {
                    if (mapper.transitionIfStale(
                            identifier.type().getCode(),
                            identifier.value(),
                            userId.value(),
                            IdentityLookupStatus.RESERVED.getCode(),
                            IdentityLookupStatus.ACTIVE.getCode(),
                            olderThan.getSeconds()
                    ) == 1) handled ++;
                } else {
                    if (mapper.deleteIfStale(
                            identifier.type().getCode(),
                            identifier.value(),
                            userId.value(),
                            IdentityLookupStatus.RESERVED.getCode(),
                            olderThan.getSeconds()
                    ) == 1) handled ++;
                }
            } catch (RuntimeException e){
                log.error("ReclaimReservedStale error of user {}: ",po.getUserId(), e);
            }

        }
        log.debug("Resolve reserved stale {}", handled);
        return handled;
    }

    /**
     * 回收崩溃遗留的 RELEASING（供定时任务调用）。
     * 对 updated_at 早于 olderThan 的 RELEASING，交给 identityExists 判断身份行是否还在：
     * 仍在（释放没有真正完成）→ 回滚为 ACTIVE；已不在（释放已完成，只差删除 lookup）→ 删除记录。
     * 两种处理都只作用于仍为 RELEASING 的行。
     * 查询不带分片键，会广播到所有分片，仅用于后台任务。
     *
     * @return 实际还原或删除的记录数
     */
    public int reclaimReleasingStale(Duration olderThan, int limit, Predicate<PendingLookup> identityExists) {
        List<IdentityLookupPO> stale = mapper.selectStale(IdentityLookupStatus.RELEASING.getCode(), olderThan.getSeconds(), limit);
        log.debug("Found releasing stale {}", stale.size());
        int handled = 0;
        for (IdentityLookupPO po : stale) {
            try {
                IdentityIdentifier identifier = IdentityIdentifierFactory.reconstitute(po.getIdentityType(), po.getIdentifier());
                ShardedId userId = ShardedId.fromId(po.getUserId());
                PendingLookup pending = new PendingLookup(identifier, userId);

                if (identityExists.test(pending)) {
                    if (mapper.transitionIfStale(
                            identifier.type().getCode(),
                            identifier.value(),
                            userId.value(),
                            IdentityLookupStatus.RELEASING.getCode(),
                            IdentityLookupStatus.ACTIVE.getCode(),
                            olderThan.getSeconds()
                    ) == 1) handled ++;
                } else {
                    if (mapper.deleteIfStale(
                            identifier.type().getCode(),
                            identifier.value(),
                            userId.value(),
                            IdentityLookupStatus.RELEASING.getCode(),
                            olderThan.getSeconds()
                    ) == 1) handled ++;
                }
            } catch (RuntimeException e){
                log.error("ReclaimReleasingStale error of user {}: ",po.getUserId(), e);
            }

        }
        log.debug("Resolve releasing stale {}", handled);
        return handled;
    }

    // -------------------------------- 内部，幂等操作

    private void idempotentInsert(IdentityIdentifier identifier, ShardedId userId, IdentityLookupStatus status) {

        mapper.insertOrTouch(identifier.type().getCode(), identifier.value(), userId.value(), status.getCode());
        // 不要依赖返回的影响行数：JDBC 默认返回"匹配行数"，
        // 别人的记录没被修改时也会返回 1，和"插入成功"分不开
        IdentityLookupPO existing = selectByIdentifier(identifier);
        if (existing == null) {
            throw new IdentifierAlreadyTakenException(identifier);   // 理论上不会发生
        }
        requireExisting(identifier, userId, status, existing);
        // 已存在的记录属于当前用户且已处于目标状态：幂等成功
    }

    private void idempotentUpdateStatus(IdentityIdentifier identifier, ShardedId userId, IdentityLookupStatus from, IdentityLookupStatus to) {
        if (tryTransition(identifier, userId, from, to)) {
            // 直接成功返回
            return;
        }
        IdentityLookupPO existing = selectByIdentifier(identifier);

        if (existing == null) {
            // 数据已经不存在
            throw new IdentifierNotExistException(identifier);
        }

        requireExisting(identifier, userId, to, existing);
        // 记录属于当前用户，且已经处于目标状态：幂等成功

        if (to == IdentityLookupStatus.RELEASING) {
            mapper.touch(identifier.type().getCode(), identifier.value(), userId.value(), to.getCode());   // 忽略返回值
        }
    }

    private void idempotentDelete(IdentityIdentifier identifier, ShardedId userId, IdentityLookupStatus status) {
        if (tryDeleteIfMatch(identifier, userId, status)) {
            return;
        }
        IdentityLookupPO existing = selectByIdentifier(identifier);
        if (existing == null) {
            // 其实已删除
            return;
        }
        if (!Objects.equals(existing.getUserId(), userId.value())) {
            // 由于lookup 业务端的入口只有插入和更新
            // 所以该方法不会由于业务端传入参数错误而导致删除的IdentityIdentifier，不是当前user的，
            // 出现二者不符，只能是并发问题，也就是IdentityIdentifier其实已经删除，又被其它user占用
            // 所以此时认为该方法幂等，而不是抛出错误
            return ;
        }
        if (existing.getStatus() != status.getCode()) {
            throw new InvalidIdentifierStatusException(identifier, IdentityLookupStatus.fromCode(existing.getStatus()));
        }
        // 属主和状态都匹配却没删掉：说明读写之间被并发修改
        // 自己的、状态也对，却没删掉：读写之间被并发改动，再删一次
        if (!tryDeleteIfMatch(identifier, userId, status)) {
            throw new ConcurrentModificationException("identity_lookup 并发修改，请重试");
        }
    }

    /** 校验已存在的记录属于该用户且处于期望状态，否则抛出对应异常。属主校验优先于状态校验 */
    private void requireExisting(IdentityIdentifier identifier, ShardedId userId, IdentityLookupStatus status, IdentityLookupPO existing) {
        if (!Objects.equals(existing.getUserId(), userId.value())) {
            throw new IdentifierNotOwnedByUserException(identifier, userId, existing.getUserId());
        }
        if (existing.getStatus() != status.getCode()) {
            throw new InvalidIdentifierStatusException(identifier, IdentityLookupStatus.fromCode(existing.getStatus()));
        }
    }

    // ------------------------------------------------------------------ 内部，简单操作

    private IdentityLookupPO newLookup(IdentityIdentifier identifier, ShardedId userId, IdentityLookupStatus status) {
        IdentityLookupPO po = new IdentityLookupPO();
        po.setIdentityType(identifier.type().getCode());
        po.setIdentifier(identifier.value());
        po.setUserId(userId.value());
        po.setStatus(status.getCode());
        return po;
    }

    private IdentityLookupPO selectByIdentifier(IdentityIdentifier identifier) {
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

    private boolean tryTransition(IdentityIdentifier identifier, ShardedId userId, IdentityLookupStatus from, IdentityLookupStatus to) {
        return mapper.transition(identifier.type().getCode(), identifier.value(), userId.value(), from, to) == 1;
    }

    private boolean tryDeleteIfMatch(IdentityIdentifier identifier, ShardedId userId, IdentityLookupStatus status) {
        return mapper.deleteIfMatch(identifier.type().getCode(), identifier.value(), userId.value(), status) == 1;
    }

}