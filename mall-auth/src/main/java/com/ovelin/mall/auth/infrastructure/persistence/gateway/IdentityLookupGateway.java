package com.ovelin.mall.auth.infrastructure.persistence.gateway;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ovelin.mall.auth.domain.exception.IdentifierAlreadyTakenException;
import com.ovelin.mall.auth.domain.model.enums.IdentityLookupStatus;
import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import com.ovelin.mall.auth.infrastructure.persistence.mapper.IdentityLookupMapper;
import com.ovelin.mall.auth.infrastructure.persistence.po.IdentityLookupPO;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * identity_lookup 的访问网关（infrastructure 内部，不对 domain / application 暴露）。
 * <p>
 * lookup 按 (identity_type, identifier) 哈希分片，与聚合（按 userId 分片）通常不在同一个库，
 * 所以它的写入与聚合落库之间没有本地事务保证，靠"占位(PENDING) → 确认(ACTIVE) / 释放"协议保证最终一致：
 * <ol>
 *   <li>{@link #reserve}：INSERT PENDING，主键冲突即标识被占用</li>
 *   <li>聚合落库成功后 {@link #confirm}；失败则 {@link #release}</li>
 *   <li>进程崩溃遗留的 PENDING 由 {@link #reclaimStale} 定时回收</li>
 * </ol>
 * reserve / confirm / release 各自独立提交（REQUIRES_NEW），不会被外层事务回滚带走，
 * 也不会被外层事务长时间持有行锁。
 */
@Component
@RequiredArgsConstructor
public class IdentityLookupGateway {

    private final IdentityLookupMapper mapper;

    /** 待回收的占位记录（identityType 为原始 code，由调用方自行转换） */
    public record PendingLookup(String identityType, String identifier, ShardedId userId) {
    }

    /**
     * 占位。同一用户对同一标识重复占位（重试）视为幂等成功。
     *
     * @throws IdentifierAlreadyTakenException 标识已被其他账号占用，或已是当前账号的已确认身份
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reserve(IdentityType type, String identifier, ShardedId userId) {
        IdentityLookupPO po = new IdentityLookupPO();
        po.setIdentityType(code(type));
        po.setIdentifier(identifier);
        po.setUserId(toLong(userId));
        po.setStatus(IdentityLookupStatus.PENDING.getCode());

        if (tryInsert(po)) {
            return;
        }
        IdentityLookupPO existing = mapper.selectOne(byKey(type, identifier));
        if (existing == null) {
            // 竞态：冲突方刚好被释放，重试一次
            if (tryInsert(po)) {
                return;
            }
            throw new IdentifierAlreadyTakenException(type, identifier);
        }
        boolean sameUserPending = Objects.equals(existing.getUserId(), po.getUserId())
                && existing.getStatus() == IdentityLookupStatus.PENDING.getCode();
        if (!sameUserPending) {
            throw new IdentifierAlreadyTakenException(type, identifier);
        }
    }

    /**
     * 确认：聚合落库成功后调用，PENDING → ACTIVE。已是本用户的 ACTIVE 视为幂等成功。
     *
     * @throws IllegalStateException 占位已丢失（例如落库太慢被回收任务清理），调用方需让本次操作失败
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void confirm(IdentityType type, String identifier, ShardedId userId) {
        if (promote(code(type), identifier, toLong(userId))) {
            return;
        }
        IdentityLookupPO existing = mapper.selectOne(byKey(type, identifier));
        boolean alreadyActive = existing != null
                && Objects.equals(existing.getUserId(), toLong(userId))
                && existing.getStatus() == IdentityLookupStatus.ACTIVE.getCode();
        if (!alreadyActive) {
            throw new IllegalStateException("登录标识占位已丢失，请重试");
        }
    }

    /** 释放：补偿 / 解绑 / 注销时调用。只删除属于该用户的记录，不存在也不报错 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(IdentityType type, String identifier, ShardedId userId) {
        mapper.delete(byKey(type, identifier)
                .eq(IdentityLookupPO::getUserId, toLong(userId)));
    }

    /** 登录读路径：标识 → userId，只认已确认的记录 */
    public Optional<ShardedId> findUserId(IdentityType type, String identifier) {
        IdentityLookupPO po = mapper.selectOne(byKey(type, identifier)
                .eq(IdentityLookupPO::getStatus, IdentityLookupStatus.ACTIVE.getCode()));
        return Optional.ofNullable(po).map(p -> toShardedId(p.getUserId()));
    }

    /**
     * 回收崩溃遗留的 PENDING（供定时任务调用）。
     * 对创建时间早于 olderThan 的 PENDING，交给 identityExists 判断身份行是否已落库：
     * 已落库 → 补确认（ACTIVE）；未落库 → 删除占位。两种处理都只作用于仍为 PENDING 的行。
     * 查询不带分片键，会广播到所有分片，仅用于后台任务。
     *
     * @return 本次处理的记录数
     */
    public int reclaimStale(Duration olderThan, int limit, Predicate<PendingLookup> identityExists) {
        List<IdentityLookupPO> stale = mapper.selectList(Wrappers.<IdentityLookupPO>lambdaQuery()
                .eq(IdentityLookupPO::getStatus, IdentityLookupStatus.PENDING.getCode())
                .apply("created_at < DATE_SUB(NOW(3), INTERVAL {0} SECOND)", olderThan.toSeconds())
                .last("LIMIT " + Math.max(1, limit)));
        int handled = 0;
        for (IdentityLookupPO po : stale) {
            PendingLookup pending = new PendingLookup(po.getIdentityType(), po.getIdentifier(),
                    toShardedId(po.getUserId()));
            if (identityExists.test(pending)) {
                promote(po.getIdentityType(), po.getIdentifier(), po.getUserId());
            } else {
                mapper.delete(Wrappers.<IdentityLookupPO>lambdaQuery()
                        .eq(IdentityLookupPO::getIdentityType, po.getIdentityType())
                        .eq(IdentityLookupPO::getIdentifier, po.getIdentifier())
                        .eq(IdentityLookupPO::getUserId, po.getUserId())
                        .eq(IdentityLookupPO::getStatus, IdentityLookupStatus.PENDING.getCode()));
            }
            handled++;
        }
        return handled;
    }

    // ------------------------------------------------------------------ 内部

    private boolean tryInsert(IdentityLookupPO po) {
        try {
            mapper.insert(po);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** PENDING → ACTIVE（条件更新），返回是否更新到行 */
    private boolean promote(String typeCode, String identifier, Long userId) {
        int rows = mapper.update(null, Wrappers.<IdentityLookupPO>lambdaUpdate()
                .set(IdentityLookupPO::getStatus, IdentityLookupStatus.ACTIVE.getCode())
                .eq(IdentityLookupPO::getIdentityType, typeCode)
                .eq(IdentityLookupPO::getIdentifier, identifier)
                .eq(IdentityLookupPO::getUserId, userId)
                .eq(IdentityLookupPO::getStatus, IdentityLookupStatus.PENDING.getCode()));
        return rows == 1;
    }

    /** 带分片键 (identity_type, identifier) 的条件 */
    private LambdaQueryWrapper<IdentityLookupPO> byKey(IdentityType type, String identifier) {
        return Wrappers.<IdentityLookupPO>lambdaQuery()
                .eq(IdentityLookupPO::getIdentityType, code(type))
                .eq(IdentityLookupPO::getIdentifier, identifier);
    }

    // 以下三处对接现有约定，按项目里 IdentityType / ShardedId 的实际 API 调整即可
    private static String code(IdentityType type) {
        return type.getCode();
    }

    private static Long toLong(ShardedId id) {
        return id.value();
    }

    private static ShardedId toShardedId(Long value) {
        return ShardedId.fromId(value);
    }
}