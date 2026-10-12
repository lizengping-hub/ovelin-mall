package com.ovelin.mall.user.domain.exception;

public class InvalidArgumentException extends UserDomainException {
    public InvalidArgumentException(String message) {
        super(ErrorCode.INVALID_ARGUMENT, message, message);   // 校验文案本来就是给用户看的
    }
}