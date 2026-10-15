package com.ovelin.mall.auth.domain.repository;

import com.ovelin.mall.auth.domain.model.aggregate.UserPassword;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;

import java.util.Optional;

public interface UserPasswordRepository {
    void add(UserPassword password);
    void update(UserPassword password);
    Optional<UserPassword> findByUserId(ShardedId userId);
    void deleteByUserId(ShardedId userId);
}