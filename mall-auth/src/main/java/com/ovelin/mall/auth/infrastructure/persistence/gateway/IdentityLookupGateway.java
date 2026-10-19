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
 * 除 {@link #findUserId} 和 {@link #insertConfirmed} 外，所有状态变更方法都是幂等的：
 * <ul>
 *   <li>转换类（reserve / confirm / beginRelease / abortRelease）：目标状态已达成且属于同一用户时直接成功；
 *       记录属于他人抛 {@link IdentifierNotOwnedByUserException}；状态不符合抛 {@link InvalidIdentifierStatusException}</li>
 *   <li>删除类（cancelReserve / completeRelease）是补偿操作，目标是"我不再持有该标识"：
 *       记录不存在、或已被他人占用都视为已完成，直接返回（否则重试任务会永远失败）；
 *       记录仍是本用户的但状态不符合，抛 {@link InvalidIdentifierStatusException}</li>
 * </ul>
 * 重复 reserve / 重复 beginRelease 会刷新 updated_at，表示该操作仍在进行中，避免被回收任务误回收。
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
     * 占位：INSERT RESERVED。同一用户对同一标识重复占位（重试）视为幂等成功，并刷新 updated_at。
     * 注意：用户注册时的重试和网络抖动都会产生不同的 userId，除非在第一次请求时就记录下 userId，
     * 否则重试会被当作"另一个用户"而被拒绝。
     *
     * @throws IdentifierNotOwnedByUserException 标识已被其他用户占用（任意状态）
     * @throws InvalidIdentifierStatusException  标识已是本用户的，但状态不是 RESERVED（已 ACTIVE / RELEASING）
     * @throws IdentifierAlreadyTakenException   极端并发：upsert 之后查不到记录（冲突方恰好被释放），调用方可重试
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
     * 或 cancelReserve 自身失败后由自动任务重试。
     * 记录不存在、或已被其他用户占用（说明本用户的占位早已被清理）都视为已完成，直接返回。
     *
     * @throws InvalidIdentifierStatusException 记录是本用户的，但已 ACTIVE / RELEASING，不能当作占位取消
     * @throws ConcurrentModificationException  记录属主和状态都匹配，但重试删除仍未成功（读写之间被并发修改），调用方可重试
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelReserve(IdentityIdentifier identifier, ShardedId userId) {
        idempotentDelete(identifier, userId, IdentityLookupStatus.RESERVED);
    }

    //   ------------------------------------------------------------------    release

    /**
     * 申请释放：ACTIVE → RELEASING（登录路由立即不可见）。
     * 已是本用户的 RELEASING 视为幂等成功，并刷新 updated_at（表示释放仍在进行）。
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
     * 完成释放：删除本用户的 RELEASING 记录。
     * 记录不存在、或已被其他用户占用（说明本用户的记录早已删除）都视为已完成，直接返回。
     *
     * @throws InvalidIdentifierStatusException 记录是本用户的，但不处于 RELEASING（必须先 {@link #beginRelease}）
     * @throws ConcurrentModificationException 记录属主和状态都匹配，但重试删除仍未成功（读写之间被并发修改），调用方可重试
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

    // ------------------------------------------------------------------ reclaim

    /**
     * 回收崩溃遗留的 RESERVED（供定时任务调用）。
     * 对 updated_at 早于 olderThan 的 RESERVED，交给 identityExists 判断身份行是否已落库：
     * 已落库 → 补确认（ACTIVE）；未落库 → 删除占位。
     * 修改时会再次校验属主、状态和 updated_at 仍早于 olderThan，
     * 所以判断期间被正常流程处理或刷新过的记录不会被误改。
     * 查询不带分片键，会广播到所有分片，仅用于后台任务。
     * 单条记录处理失败只记录日志，不影响其他记录。
     *
     * @return 实际被补确认或删除的记录数（判断期间被别人处理过而跳过的不计入）
     */
    public int reclaimReservedStale(Duration olderThan, int limit, Predicate<PendingLookup> identityExists) {
        return reclaimStale(IdentityLookupStatus.RESERVED, olderThan, limit, identityExists);
    }

    /**
     * 回收崩溃遗留的 RELEASING（供定时任务调用）。
     * 对 updated_at 早于 olderThan 的 RELEASING，交给 identityExists 判断身份行是否还在：
     * 仍在（释放没有真正完成）→ 回滚为 ACTIVE；已不在（释放已完成，只差删除 lookup）→ 删除记录。
     * 修改时会再次校验属主、状态和 updated_at 仍早于 olderThan，
     * 所以判断期间被正常流程处理或刷新过的记录不会被误改。
     * 查询不带分片键，会广播到所有分片，仅用于后台任务。
     * 单条记录处理失败只记录日志，不影响其他记录。
     *
     * @return 实际被还原或删除的记录数（判断期间被别人处理过而跳过的不计入）
     */
    public int reclaimReleasingStale(Duration olderThan, int limit, Predicate<PendingLookup> identityExists) {
        return reclaimStale(IdentityLookupStatus.RELEASING, olderThan, limit, identityExists);
    }

    /**
     * 回收的公共流程。只尝试一次、不做幂等：条件没匹配上说明记录已被正常流程处理或刷新，直接放过，
     * 若仍然过期，下一轮扫描会再次选中。
     * 注意日志里只打印标识类型和 userId，不打印标识值（邮箱、手机号属于个人信息）。
     */
    private int reclaimStale(IdentityLookupStatus staleStatus, Duration olderThan, int limit,
                             Predicate<PendingLookup> identityExists) {
        long olderThanSeconds = olderThan.getSeconds();
        List<IdentityLookupPO> stale = mapper.selectStale(staleStatus.getCode(), olderThanSeconds, limit);
        log.debug("Found {} stale {} lookups", stale.size(), staleStatus);

        int handled = 0;
        for (IdentityLookupPO po : stale) {
            try {
                IdentityIdentifier identifier = IdentityIdentifierFactory.reconstitute(po.getIdentityType(), po.getIdentifier());
                ShardedId userId = ShardedId.fromId(po.getUserId());
                PendingLookup pending = new PendingLookup(identifier, userId);

                int affected;
                if (identityExists.test(pending)) {
                    // RESERVED：身份行已落库 → 补确认；RELEASING：身份行仍在 → 回滚。目标状态都是 ACTIVE
                    affected = mapper.transitionIfStale(
                            identifier.type().getCode(),
                            identifier.value(),
                            userId.value(),
                            staleStatus.getCode(),
                            IdentityLookupStatus.ACTIVE.getCode(),
                            olderThanSeconds);
                } else {
                    affected = mapper.deleteIfStale(
                            identifier.type().getCode(),
                            identifier.value(),
                            userId.value(),
                            staleStatus.getCode(),
                            olderThanSeconds);
                }
                if (affected == 1) {
                    handled++;
                }
            } catch (RuntimeException e) {
                log.error("Reclaim stale {} lookup failed, identityType={}, userId={}",
                        staleStatus, po.getIdentityType(), po.getUserId(), e);
            }
        }
        log.debug("Reclaimed {} of {} stale {} lookups", handled, stale.size(), staleStatus);
        return handled;
    }

    // -------------------------------- 内部，幂等操作

    private void idempotentInsert(IdentityIdentifier identifier, ShardedId userId, IdentityLookupStatus status) {
        // INSERT ... ON DUPLICATE KEY UPDATE：冲突时只有"同一用户、同一状态"才刷新 updated_at，别人的记录原样不动。
        // 冲突时直接加 X 锁，避免"INSERT 冲突持有 S 锁 → 再 UPDATE 升级 X 锁"造成的并发死锁。
        mapper.insertOrTouch(identifier.type().getCode(), identifier.value(), userId.value(), status.getCode());
        // 不要依赖返回的影响行数：JDBC 默认返回"匹配行数"，
        // 别人的记录没被修改时也会返回 1，和"插入成功"分不开，所以必须再读一次校验属主和状态
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
            throw new IdentifierNotExistException(identifier);
        }

        requireExisting(identifier, userId, to, existing);
        // 记录属于当前用户，且已经处于目标状态：幂等成功

        if (to == IdentityLookupStatus.RELEASING) {
            // 重复的 beginRelease 说明释放仍在进行，刷新 updated_at，避免被回收任务误回收。
            // 条件里带了属主和状态，读写之间被改动时影响 0 行，所以忽略返回值
            mapper.touch(identifier.type().getCode(), identifier.value(), userId.value(), to.getCode());
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
            // lookup 在业务侧的入口只有插入和状态更新，不会因为调用方传错参数而出现属主不符。
            // 出现属主不符只能是并发导致：本用户的记录其实早已删除，标识又被其他用户占用。
            // 补偿目标（我不再持有该标识）已经达成，视为幂等成功，而不是抛异常。
            return;
        }
        if (existing.getStatus() != status.getCode()) {
            throw new InvalidIdentifierStatusException(identifier, IdentityLookupStatus.fromCode(existing.getStatus()));
        }
        // 属主和状态都匹配却没删掉：读写之间被并发改动，再删一次
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