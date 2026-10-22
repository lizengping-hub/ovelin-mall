package com.ovelin.mall.auth.domain.exception;

import com.ovelin.mall.auth.domain.exception.face.AuthDomainException;
import com.ovelin.mall.auth.domain.exception.face.AuthErrorCode;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;

public class IdentityNotExistException extends AuthDomainException {
    public IdentityNotExistException(ShardedId identityId) {
        super(AuthErrorCode.IDENTITY_NOT_EXIST, "身份 %s 不存在".formatted(identityId));
    }
}
