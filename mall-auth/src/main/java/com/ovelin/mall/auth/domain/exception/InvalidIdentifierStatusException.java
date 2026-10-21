package com.ovelin.mall.auth.domain.exception;

import com.ovelin.mall.auth.domain.exception.face.AuthDomainException;
import com.ovelin.mall.auth.domain.exception.face.AuthErrorCode;
import com.ovelin.mall.auth.domain.model.enums.IdentityLookupStatus;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifier;
import lombok.Getter;

/** 登录标识已被其他账号占用（全局唯一约束冲突） */
@Getter
public class InvalidIdentifierStatusException extends AuthDomainException {

    public InvalidIdentifierStatusException(IdentityIdentifier identifier, IdentityLookupStatus status) {
        super(AuthErrorCode.INVALID_IDENTIFIER_STATUS, "Invalid identity identifier status " + status.name() + " of " + identifier);
    }

}