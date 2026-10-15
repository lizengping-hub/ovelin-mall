package com.ovelin.mall.common.exception.global.face;

import com.ovelin.mall.common.exception.ErrorCode;

public enum GlobalErrorCode implements ErrorCode {
    CONCURRENT_UPDATE(),
    INVALID_ARGUMENT(),
    INVALID_STATUS(),
    RESOURCE_NOT_FOUND();

}
