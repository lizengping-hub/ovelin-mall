package com.ovelin.mall.user.api;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;

public interface UserApi {
    ShardedId createUser(CreateUserCommand command);
}