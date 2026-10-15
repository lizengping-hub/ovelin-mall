package com.ovelin.mall.auth.domain.exception;

import com.ovelin.mall.auth.domain.exception.face.AuthDomainException;
import com.ovelin.mall.auth.domain.exception.face.AuthErrorCode;

/** 身份不存在和密码错误统一用它，避免泄露账号是否存在 */
public class PasswordFailedException extends AuthDomainException {
    public PasswordFailedException() {
        super(AuthErrorCode.AUTHENTICATION_FAILED, "密码错误");
    }
}