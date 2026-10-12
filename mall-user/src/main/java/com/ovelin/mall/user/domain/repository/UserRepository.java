package com.ovelin.mall.user.domain.repository;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.user.domain.model.aggregate.User;

public interface UserRepository {
    User findById(ShardedId id);
    void add(User user);
    void update(User user);
}
