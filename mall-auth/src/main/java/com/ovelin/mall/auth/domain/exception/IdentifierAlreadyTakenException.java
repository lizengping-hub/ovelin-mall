package com.ovelin.mall.auth.domain.exception;

import com.ovelin.mall.auth.domain.exception.face.AuthDomainException;
import com.ovelin.mall.auth.domain.exception.face.AuthErrorCode;
import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifier;
import lombok.Getter;

/** 登录标识已被占用（全局唯一约束冲突）</br>
 *  此异常不区分登录标识是被谁占用的，</br>
 *  更多的情况下，该异常会被细化为IdentifierNotOwnedByUserException或InvalidIdentifierStatusException
 * */
@Getter
public class IdentifierAlreadyTakenException extends AuthDomainException {

    public IdentifierAlreadyTakenException(IdentityIdentifier identifier) {
        super(AuthErrorCode.IDENTIFIER_ALREADY_TAKEN, "登录标识已被占用: " + identifier);
    }

}