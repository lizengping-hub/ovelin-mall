package com.ovelin.mall.common.sharding.core.api;

import com.ovelin.mall.common.sharding.core.ov.ShardId;

public interface TableIndexResolver {
    int resolve(ShardId shardId, int dsCount, int tableCount);
}
