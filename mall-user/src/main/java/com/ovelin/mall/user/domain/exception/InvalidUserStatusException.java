package com.ovelin.mall.user.domain.exception;
import com.ovelin.mall.user.domain.exception.face.UserDomainException;
import com.ovelin.mall.user.domain.exception.face.UserErrorCode;
import com.ovelin.mall.user.domain.model.enums.UserStatus;
import lombok.Getter;

@Getter
public class InvalidUserStatusException extends UserDomainException {
    private final Long userId;
    private final UserStatus current;     // 当前状态
    private final String operation;       // 想做的操作，如 "changeEmail"
    ;
    public InvalidUserStatusException(Long userId, UserStatus current, String operation) {
        super(UserErrorCode.INVALID_USER_STATUS, "user %d in status %s cannot %s".formatted(userId, current, operation));
        this.userId = userId;
        this.current = current;
        this.operation = operation;
    }

}
