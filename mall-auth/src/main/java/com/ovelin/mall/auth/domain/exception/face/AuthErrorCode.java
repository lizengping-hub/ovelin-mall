package com.ovelin.mall.auth.domain.exception.face;

import com.ovelin.mall.common.exception.ErrorCode;

public enum AuthErrorCode implements ErrorCode {
    IDENTIFIER_ALREADY_TAKEN,
    INVALID_IDENTIFIER_STATUS,
    IDENTIFIER_ALREADY_BINDED,
    ACCOUNT_LOCKED,
    ACCOUNT_NOT_FOUND,
    MFA_FACTOR_ALREADY_EXIST, AUTHENTICATION_FAILED

}