package com.ovelin.mall.auth.domain.exception;

import com.ovelin.mall.auth.domain.exception.face.AuthDomainException;
import com.ovelin.mall.auth.domain.exception.face.AuthErrorCode;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;

public class AccountNotFoundException extends AuthDomainException {
    public AccountNotFoundException(ShardedId userid) {
        super(AuthErrorCode.ACCOUNT_NOT_FOUND, "Account "+ userid + " not found");
    }
}
