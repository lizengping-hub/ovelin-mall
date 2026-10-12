package com.ovelin.mall.user.domain.exception;

public class UserNotFoundException extends UserDomainException {
    public UserNotFoundException(Long userId) {
        super(ErrorCode.USER_NOT_FOUND, "user %d not found".formatted(userId));
        // 对外："用户不存在"，id 只进日志
    }
}