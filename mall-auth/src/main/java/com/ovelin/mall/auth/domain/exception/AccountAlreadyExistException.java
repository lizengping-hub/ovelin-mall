package com.ovelin.mall.auth.domain.exception;

import com.ovelin.mall.auth.domain.exception.face.AuthDomainException;
import com.ovelin.mall.auth.domain.exception.face.AuthErrorCode;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;

public class AccountAlreadyExistException extends AuthDomainException {
    public AccountAlreadyExistException(ShardedId userId){
        super(AuthErrorCode.ACCOUNT_ALREADY_EXIST, "Account of user " + userId +" already exist");
    }
}
