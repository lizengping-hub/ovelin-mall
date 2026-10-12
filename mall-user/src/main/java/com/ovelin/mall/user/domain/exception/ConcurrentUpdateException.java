package com.ovelin.mall.user.domain.exception;

import com.ovelin.mall.user.domain.model.aggregate.User;

public class ConcurrentUpdateException extends UserDomainException{
    public ConcurrentUpdateException(User user) {
        super(ErrorCode.CONCURRENT_UPDATE,
                "user %d was modified concurrently, stale version %d"
                        .formatted(user.getId().value(), user.getVersion()));
    }
}
