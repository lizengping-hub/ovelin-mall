package com.ovelin.mall.user.domain.exception;

import com.ovelin.mall.user.domain.exception.face.UserDomainException;
import com.ovelin.mall.user.domain.exception.face.UserErrorCode;
import com.ovelin.mall.user.domain.model.aggregate.User;

public class UserConcurrentUpdateException extends UserDomainException {
    public UserConcurrentUpdateException(User user) {
        super(UserErrorCode.USER_CONCURRENT_UPDATE,
                "user %d was modified concurrently, stale version %d"
                        .formatted(user.getId().value(), user.getVersion()));
    }
}
