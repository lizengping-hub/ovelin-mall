package com.ovelin.mall.auth.domain.exception;

import com.ovelin.mall.auth.domain.exception.face.AuthDomainException;
import com.ovelin.mall.auth.domain.exception.face.AuthErrorCode;
import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import lombok.Getter;

/** 登录标识已被其他账号占用（全局唯一约束冲突） */
@Getter
public class IdentifierAlreadyTakenException extends AuthDomainException {

    public IdentifierAlreadyTakenException(IdentityType type, String identifier) {
        super(AuthErrorCode.IDENTIFIER_ALREADY_TAKEN, "登录标识已被占用: " + type +"/"  +identifier);
    }

}