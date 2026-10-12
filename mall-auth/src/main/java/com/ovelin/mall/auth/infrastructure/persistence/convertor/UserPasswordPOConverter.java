package com.ovelin.mall.auth.infrastructure.persistence.convertor;

import com.ovelin.mall.auth.domain.model.aggregate.UserPassword;
import com.ovelin.mall.auth.infrastructure.persistence.po.UserPasswordPO;

public final class UserPasswordPOConverter {

    private UserPasswordPOConverter() {
    }

    public static UserPassword toDomain(UserPasswordPO po) {
        return UserPassword.restore(
                po.getUserId(), po.getPasswordHash(), po.getPasswordChangedAt(),
                po.getFailedAttempts(), po.getLockedUntil());
    }

    public static UserPasswordPO toPO(UserPassword password) {
        UserPasswordPO po = new UserPasswordPO();
        po.setUserId(password.getUserId());
        po.setPasswordHash(password.getPasswordHash());
        po.setPasswordChangedAt(password.getPasswordChangedAt());
        po.setFailedAttempts(password.getFailedAttempts());
        po.setLockedUntil(password.getLockedUntil());
        return po;
    }
}