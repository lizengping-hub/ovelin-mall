package com.ovelin.mall.auth.domain.model.aggregate;

import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import com.ovelin.mall.auth.domain.model.enums.MfaFactorStatus;
import com.ovelin.mall.auth.domain.model.enums.MfaFactorType;
import com.ovelin.mall.common.exception.global.InvalidArgumentException;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import lombok.Getter;

import java.time.Instant;

/**
 * 二次验证因子：LoginAccount 聚合内的实体。
 * 创建与状态修改方法均为包级可见，外部只能通过 {@link LoginAccount} 操作；
 * 修改方法返回"是否发生变化"，供聚合根决定是否记入变更集。
 */
@Getter
public class MfaFactor {

    private final ShardedId id;
    private final ShardedId userId;
    private final MfaFactorType type;
    private final ShardedId identityId;
    private final String secretEncrypted;
    private MfaFactorStatus status;
    private Instant verifiedAt;
    private Instant lastUsedAt;

    private MfaFactor(ShardedId id, ShardedId userId, MfaFactorType type, ShardedId identityId,
                      String secretEncrypted, MfaFactorStatus status, Instant verifiedAt, Instant lastUsedAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.identityId = identityId;
        this.secretEncrypted = secretEncrypted;
        this.status = status;
        this.verifiedAt = verifiedAt;
        this.lastUsedAt = lastUsedAt;
    }

    /** 短信/邮箱因子：引用一个已验证的身份，不存密钥。仅由 LoginAccount 调用 */
    static MfaFactor forIdentity(ShardedId id, UserIdentity identity) {
        if (!identity.canReceiveCode()) {
            throw new IllegalStateException("该身份不能用于二次验证：必须是已验证的手机号或邮箱");
        }
        requireSameShard(id, identity.getUserId());
        MfaFactorType type = identity.type() == IdentityType.PHONE ? MfaFactorType.SMS : MfaFactorType.EMAIL;
        return new MfaFactor(id, identity.getUserId(), type, identity.getId(), null,
                MfaFactorStatus.PENDING, null, null);
    }

    /** TOTP / WebAuthn / 恢复码：secretEncrypted 由应用服务加密后传入。仅由 LoginAccount 调用 */
    static MfaFactor withSecret(ShardedId id, ShardedId userId, MfaFactorType type, String secretEncrypted) {
        if (type.referencesIdentity()) {
            throw new InvalidArgumentException("短信/邮箱因子请使用 addIdentityFactor");
        }
        requireSameShard(id, userId);
        return new MfaFactor(id, userId, type, null, secretEncrypted, MfaFactorStatus.PENDING, null, null);
    }

    /** 仅供持久化层还原，不做校验 */
    public static MfaFactor reconstitute(ShardedId id, ShardedId userId, MfaFactorType type, ShardedId identityId,
                                         String secretEncrypted, MfaFactorStatus status,
                                         Instant verifiedAt, Instant lastUsedAt) {
        return new MfaFactor(id, userId, type, identityId, secretEncrypted, status, verifiedAt, lastUsedAt);
    }

    private static void requireSameShard(ShardedId id, ShardedId userId) {
        if (!id.hasSameShard(userId)) {
            throw new InvalidArgumentException("因子记录必须与用户落在同一分片");
        }
    }

    /** @return 是否发生变化 */
    boolean enable(Instant at) {
        boolean changed = status != MfaFactorStatus.ENABLED;
        this.status = MfaFactorStatus.ENABLED;
        if (verifiedAt == null) {
            this.verifiedAt = at;
            changed = true;
        }
        return changed;
    }

    /** @return 是否发生变化 */
    boolean disable() {
        if (status == MfaFactorStatus.DISABLED) {
            return false;
        }
        this.status = MfaFactorStatus.DISABLED;
        return true;
    }

    void markUsed(Instant at) {
        this.lastUsedAt = at;
    }

    boolean references(ShardedId identityId) {
        return this.identityId != null && this.identityId.equals(identityId);
    }

    public boolean isEnabled() {
        return status == MfaFactorStatus.ENABLED;
    }

    public boolean isDisabled() {
        return status == MfaFactorStatus.DISABLED;
    }

    @Override
    public String toString() {
        return "MfaFactor[userId = %s, type = %s, identityId = %s, secretEncrypted = %s]".formatted(userId, type, identityId, secretEncrypted);
    }
}