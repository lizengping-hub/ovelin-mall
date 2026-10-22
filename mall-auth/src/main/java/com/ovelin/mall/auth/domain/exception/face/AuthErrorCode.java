package com.ovelin.mall.auth.domain.exception.face;

import com.ovelin.mall.common.exception.ErrorCode;

public enum AuthErrorCode implements ErrorCode {
    INVALID_IDENTIFIER_STATUS,
    IDENTIFIER_ALREADY_TAKEN,
    IDENTIFIER_NOT_EXIST,
    IDENTIFIER_ALREADY_BOUND,

    ACCOUNT_LOCKED,
    ACCOUNT_NOT_FOUND,
    IDENTITY_NOT_EXIST,
    MFA_FACTOR_ALREADY_EXIST,
    MFA_FACTOR_NOT_EXIST,
    AUTHENTICATION_FAILED

}