package com.ovelin.mall.common.sharding.core.ddd;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;

public interface ShardedAggregateRoot<T extends ShardedId> extends ShardedEntity<T> {
}
