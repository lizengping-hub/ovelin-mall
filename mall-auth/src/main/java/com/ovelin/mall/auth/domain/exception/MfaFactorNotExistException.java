package com.ovelin.mall.auth.domain.exception;

import com.ovelin.mall.auth.domain.exception.face.AuthDomainException;
import com.ovelin.mall.auth.domain.exception.face.AuthErrorCode;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;

public class MfaFactorNotExistException extends AuthDomainException {
    public MfaFactorNotExistException(ShardedId factorId){
        super(AuthErrorCode.MFA_FACTOR_NOT_EXIST, "二次验证因子["+factorId+"]不存在");
    }
}
