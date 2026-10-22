package com.ovelin.mall.auth.domain.exception;

import com.ovelin.mall.auth.domain.exception.face.AuthDomainException;
import com.ovelin.mall.auth.domain.exception.face.AuthErrorCode;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;

public class AccountStatusChangedException extends AuthDomainException {
    public AccountStatusChangedException(ShardedId userId){
        super(AuthErrorCode.ACCOUNT_STATUS_CHANGED,  "Account status of user " + userId + "has changed");
    }
}
