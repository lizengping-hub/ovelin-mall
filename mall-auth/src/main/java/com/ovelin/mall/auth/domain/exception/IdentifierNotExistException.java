package com.ovelin.mall.auth.domain.exception;

import com.ovelin.mall.auth.domain.exception.face.AuthDomainException;
import com.ovelin.mall.auth.domain.exception.face.AuthErrorCode;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifier;
import lombok.Getter;

/** 登录标识已被其他账号占用（全局唯一约束冲突） */
@Getter
public class IdentifierNotExistException extends AuthDomainException {

    public IdentifierNotExistException(IdentityIdentifier identifier) {
        super(AuthErrorCode.IDENTIFIER_ALREADY_TAKEN, "登录标识不存在: " + identifier);
    }

}