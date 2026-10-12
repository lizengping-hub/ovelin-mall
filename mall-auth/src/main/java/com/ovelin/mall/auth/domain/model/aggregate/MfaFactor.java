package com.ovelin.mall.auth.domain.model.aggregate;

import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import com.ovelin.mall.auth.domain.model.enums.MfaFactorStatus;
import com.ovelin.mall.auth.domain.model.enums.MfaFactorType;
import lombok.Getter;

import java.time.Instant;

@Getter
public class MfaFactor {

    private final long id;
    private final long userId;
    private final MfaFactorType type;
    private final Long identityId;
    private final String secretEncrypted;
    private MfaFactorStatus status;
    private Instant verifiedAt;
    private Instant lastUsedAt;

    private MfaFactor(long id, long userId, MfaFactorType type, Long identityId, String secretEncrypted,
                      MfaFactorStatus status, Instant verifiedAt, Instant lastUsedAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.identityId = identityId;
        this.secretEncrypted = secretEncrypted;
        this.status = status;
        this.verifiedAt = verifiedAt;
        this.lastUsedAt = lastUsedAt;
    }

    /** 短信/邮箱因子：引用一个已验证的身份，不存密钥 */
    public static MfaFactor forIdentity(long id, UserIdentity identity) {
        if (!identity.canReceiveCode()) {
            throw new IllegalStateException("该身份不能用于二次验证：必须是已验证的手机号或邮箱");
        }
        MfaFactorType type = identity.getType() == IdentityType.PHONE ? MfaFactorType.SMS : MfaFactorType.EMAIL;
        return new MfaFactor(id, identity.getUserId(), type, identity.getId(), null,
                MfaFactorStatus.PENDING, null, null);
    }

    /** TOTP / WebAuthn / 恢复码：secretEncrypted 由应用服务加密后传入 */
    public static MfaFactor withSecret(long id, long userId, MfaFactorType type, String secretEncrypted) {
        if (type.referencesIdentity()) {
            throw new IllegalArgumentException("短信/邮箱因子请使用 forIdentity");
        }
        return new MfaFactor(id, userId, type, null, secretEncrypted, MfaFactorStatus.PENDING, null, null);
    }

    public static MfaFactor restore(long id, long userId, MfaFactorType type, Long identityId,
                                    String secretEncrypted, MfaFactorStatus status,
                                    Instant verifiedAt, Instant lastUsedAt) {
        return new MfaFactor(id, userId, type, identityId, secretEncrypted, status, verifiedAt, lastUsedAt);
    }

    /** 用户完成首次验证后启用 */
    public void enable(Instant at) {
        this.status = MfaFactorStatus.ENABLED;
        if (verifiedAt == null) {
            this.verifiedAt = at;
        }
    }

    public void disable() {
        this.status = MfaFactorStatus.DISABLED;
    }

    public void markUsed(Instant at) {
        this.lastUsedAt = at;
    }

    public boolean isEnabled() {
        return status == MfaFactorStatus.ENABLED;
    }
}