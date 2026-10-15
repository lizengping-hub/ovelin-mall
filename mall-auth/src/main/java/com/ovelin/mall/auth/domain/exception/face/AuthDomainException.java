package com.ovelin.mall.auth.domain.exception.face;

import com.ovelin.mall.common.exception.DomainException;
import lombok.Getter;

@Getter
public abstract class AuthDomainException extends DomainException {
    protected AuthDomainException(AuthErrorCode code, String message) {
        super(code, message);
    }
}