package com.ovelin.mall.auth.infrastructure.persistence.convertor;

import com.ovelin.mall.auth.domain.model.aggregate.UserPassword;
import com.ovelin.mall.auth.infrastructure.persistence.po.UserPasswordPO;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;

public final class UserPasswordPOConverter {

    private UserPasswordPOConverter() {
    }

    public static UserPassword toDomain(UserPasswordPO po) {
        return UserPassword.reconstitute(
                ShardedId.fromId(po.getUserId()), po.getPasswordHash(), po.getPasswordChangedAt(),
                po.getFailedAttempts(), po.getLockedUntil());
    }

    public static UserPasswordPO toPO(UserPassword password) {
        UserPasswordPO po = new UserPasswordPO();
        po.setUserId(password.getUserId().value());
        po.setPasswordHash(password.getEncodedPassword());
        po.setPasswordChangedAt(password.getPasswordChangedAt());
        po.setFailedAttempts(password.getFailedAttempts());
        po.setLockedUntil(password.getLockedUntil());
        return po;
    }
}