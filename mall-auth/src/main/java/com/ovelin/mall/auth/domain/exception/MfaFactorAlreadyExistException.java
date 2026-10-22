package com.ovelin.mall.auth.domain.exception;

import com.ovelin.mall.auth.domain.exception.face.AuthDomainException;
import com.ovelin.mall.auth.domain.exception.face.AuthErrorCode;
import com.ovelin.mall.auth.domain.model.aggregate.UserMfaFactor;

public class MfaFactorAlreadyExistException extends AuthDomainException {
    public MfaFactorAlreadyExistException(UserMfaFactor factor) {
        super(AuthErrorCode.MFA_FACTOR_ALREADY_EXIST, factor + "已存在");
    }
}
