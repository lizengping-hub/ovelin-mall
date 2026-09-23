package com.ovelin.mall.sharding.starter.api.ddd;

import com.ovelin.mall.sharding.starter.api.ShardedId;

public interface ShardedAggregateRoot<T extends ShardedId> extends ShardedEntity<T> {
}
