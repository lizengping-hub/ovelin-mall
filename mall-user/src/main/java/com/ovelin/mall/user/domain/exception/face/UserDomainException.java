package com.ovelin.mall.user.domain.exception.face;


import com.ovelin.mall.common.exception.DomainException;

public class UserDomainException extends DomainException {

    protected UserDomainException(UserErrorCode code, String message) {
        super(code,message);
    }

    @Override
    public UserErrorCode getErrorCode(){
        return (UserErrorCode) super.getErrorCode();
    }
}