package com.ovelin.mall.user.api;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;

public interface UserQueryService {
    UserDTO findById(Long id);
}
