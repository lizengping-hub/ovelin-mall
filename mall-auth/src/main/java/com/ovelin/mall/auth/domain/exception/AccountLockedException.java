package com.ovelin.mall.auth.domain.exception;

import com.ovelin.mall.auth.domain.exception.face.AuthDomainException;
import com.ovelin.mall.auth.domain.exception.face.AuthErrorCode;

import java.time.Instant;

public class AccountLockedException extends AuthDomainException {
    public AccountLockedException(Instant lockedUntil) {
        super(AuthErrorCode.INVALID_ACCOUNT_STATUS, "账号已锁定，解锁时间: " + lockedUntil);

    }
}