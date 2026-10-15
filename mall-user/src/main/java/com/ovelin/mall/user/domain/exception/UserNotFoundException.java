package com.ovelin.mall.user.domain.exception;

import com.ovelin.mall.user.domain.exception.face.UserDomainException;
import com.ovelin.mall.user.domain.exception.face.UserErrorCode;

public class UserNotFoundException extends UserDomainException {
    public UserNotFoundException(Long userId) {
        super(UserErrorCode.USER_NOT_FOUND, "user %d not found".formatted(userId));
    }
}