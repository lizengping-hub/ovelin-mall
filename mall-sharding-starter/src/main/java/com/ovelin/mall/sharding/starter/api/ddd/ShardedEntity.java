package com.ovelin.mall.sharding.starter.api.ddd;

import com.ovelin.mall.ddd.kernel.Entity;
import com.ovelin.mall.sharding.starter.api.ShardedId;

public interface ShardedEntity<T extends ShardedId> extends Entity<T> {
    T getId();
}
