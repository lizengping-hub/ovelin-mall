package com.ovelin.mall.common.exception;

import lombok.Getter;

/**
 * errorCode：错误码 </br>
 * message: 错误信息</br>
 */
@Getter
public abstract class DomainException extends RuntimeException {
    private final ErrorCode errorCode;

    protected DomainException(ErrorCode code, String message) {
        super(message);
        this.errorCode = code;
    }
}