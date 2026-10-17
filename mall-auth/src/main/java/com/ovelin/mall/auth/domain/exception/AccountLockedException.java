package com.ovelin.mall.auth.domain.exception;

import com.ovelin.mall.auth.domain.exception.face.AuthDomainException;
import com.ovelin.mall.auth.domain.exception.face.AuthErrorCode;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;

import java.time.Instant;

public class AccountLockedException extends AuthDomainException {
    public AccountLockedException(ShardedId userId, Instant lockedUntil) {
        super(AuthErrorCode.ACCOUNT_LOCKED, "账号["+userId+"]已锁定，解锁时间: " + lockedUntil);

    }
}