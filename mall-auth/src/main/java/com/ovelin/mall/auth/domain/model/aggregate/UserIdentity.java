package com.ovelin.mall.auth.domain.model.aggregate;

import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import lombok.Getter;

import java.time.Instant;

@Getter
public class UserIdentity {

    private final long id;
    private final long userId;
    private final IdentityType type;
    private final String identifier;
    private boolean primary;
    private Instant verifiedAt;
    private Instant lastLoginAt;

    private UserIdentity(long id, long userId, IdentityType type, String identifier,
                         boolean primary, Instant verifiedAt, Instant lastLoginAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.identifier = identifier;
        this.primary = primary;
        this.verifiedAt = verifiedAt;
        this.lastLoginAt = lastLoginAt;
    }

    public static UserIdentity register(long id, long userId, IdentityType type, String rawIdentifier,
                                        boolean primary) {
        String identifier = type.normalize(rawIdentifier);
        if (identifier.isEmpty()) {
            throw new IllegalArgumentException("登录标识不能为空");
        }
        return new UserIdentity(id, userId, type, identifier, primary, null, null);
    }

    /** 仅供持久化层还原，不做标准化和校验 */
    public static UserIdentity restore(long id, long userId, IdentityType type, String identifier,
                                       boolean primary, Instant verifiedAt, Instant lastLoginAt) {
        return new UserIdentity(id, userId, type, identifier, primary, verifiedAt, lastLoginAt);
    }

    public void markVerified(Instant at) {
        if (verifiedAt == null) {
            verifiedAt = at;
        }
    }

    public void recordLogin(Instant at) {
        this.lastLoginAt = at;
    }

    public void markPrimary() {
        this.primary = true;
    }

    public void unmarkPrimary() {
        this.primary = false;
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    /** 能否用来收二次验证码：必须是手机/邮箱且已验证 */
    public boolean canReceiveCode() {
        return type.canReceiveCode() && isVerified();
    }
}