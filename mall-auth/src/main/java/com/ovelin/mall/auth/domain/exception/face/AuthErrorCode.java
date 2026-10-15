package com.ovelin.mall.auth.domain.exception.face;

import com.ovelin.mall.common.exception.ErrorCode;

public enum AuthErrorCode implements ErrorCode {
    IDENTIFIER_ALREADY_TAKEN,
    INVALID_ACCOUNT_STATUS,
    AUTHENTICATION_FAILED
}