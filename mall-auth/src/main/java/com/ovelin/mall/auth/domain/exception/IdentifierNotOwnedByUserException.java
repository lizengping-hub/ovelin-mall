package com.ovelin.mall.auth.domain.exception;

import com.ovelin.mall.auth.domain.exception.face.AuthDomainException;
import com.ovelin.mall.auth.domain.exception.face.AuthErrorCode;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifier;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import lombok.Getter;

/** 登录标识已被其他账号占用（全局唯一约束冲突） */
@Getter
public class IdentifierNotOwnedByUserException extends AuthDomainException {

    public IdentifierNotOwnedByUserException(IdentityIdentifier identifier, ShardedId currentUserId, Long anotherUserId) {
        super(AuthErrorCode.IDENTIFIER_ALREADY_TAKEN, "Identity identifier " + identifier + "'s owner is " + anotherUserId + " not " +currentUserId);
    }

}