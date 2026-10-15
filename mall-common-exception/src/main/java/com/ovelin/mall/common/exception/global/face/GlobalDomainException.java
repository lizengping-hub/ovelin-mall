package com.ovelin.mall.common.exception.global.face;

import com.ovelin.mall.common.exception.DomainException;

public abstract class GlobalDomainException extends DomainException {
    protected GlobalDomainException(GlobalErrorCode code, String message) {
        super(code, message);
    }
    @Override
    public GlobalErrorCode getErrorCode(){
        return (GlobalErrorCode)super.getErrorCode();
    }
}
