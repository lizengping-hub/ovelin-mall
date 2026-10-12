package com.ovelin.mall.user.domain.exception;

import lombok.Getter;

@Getter
public class UserDomainException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String clientMessage;
    public UserDomainException(ErrorCode code, String message) {
        this(code, message, code.clientMessage());
    }

    protected UserDomainException(ErrorCode code, String message, String clientMessage) {
        super(message);
        this.errorCode = code;
        this.clientMessage = clientMessage;
    }
}