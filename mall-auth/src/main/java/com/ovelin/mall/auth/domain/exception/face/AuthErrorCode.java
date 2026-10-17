package com.ovelin.mall.auth.domain.exception.face;

import com.ovelin.mall.common.exception.ErrorCode;

public enum AuthErrorCode implements ErrorCode {
    IDENTIFIER_ALREADY_TAKEN,
    ACCOUNT_LOCKED,
    ACCOUNT_NOT_FOUND,
    AUTHENTICATION_FAILED

}