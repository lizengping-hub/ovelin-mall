package com.ovelin.mall.auth.domain.exception;

import com.ovelin.mall.auth.domain.exception.face.AuthDomainException;
import com.ovelin.mall.auth.domain.exception.face.AuthErrorCode;

public class VerificationFailedException extends AuthDomainException {
    public VerificationFailedException() {
        super(AuthErrorCode.AUTHENTICATION_FAILED,"验证码错误或已过期");
    }
}