package com.ovelin.mall.user.domain.exception.face;


import com.ovelin.mall.common.exception.ErrorCode;

public enum UserErrorCode implements ErrorCode {
    USER_CONCURRENT_UPDATE(),
    INVALID_USER_STATUS(),
    USER_NOT_FOUND();
}