package com.ovelin.mall.auth.domain.exception;

import com.ovelin.mall.auth.domain.exception.face.AuthDomainException;
import com.ovelin.mall.auth.domain.exception.face.AuthErrorCode;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifier;


public class IdentifierAlreadyBoundException extends AuthDomainException {
    public IdentifierAlreadyBoundException(IdentityIdentifier identifier){
        super(AuthErrorCode.IDENTIFIER_ALREADY_BOUND, identifier + " already added");
    }
}

