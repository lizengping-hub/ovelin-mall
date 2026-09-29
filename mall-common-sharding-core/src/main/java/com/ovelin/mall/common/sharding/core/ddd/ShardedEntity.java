package com.ovelin.mall.common.sharding.core.ddd;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.ddd.kernel.Entity;

public interface ShardedEntity<T extends ShardedId> extends Entity<T> {
    T getId();
}
