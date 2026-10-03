package com.ovelin.mall.common.sharding.core.core;

import com.ovelin.mall.common.sharding.core.api.TableIndexResolver;
import com.ovelin.mall.common.sharding.core.ov.ShardId;

public class TableIndexResolverImpl implements TableIndexResolver {
    @Override
    public int resolve(ShardId shardId, int dsCount, int tableCount) {
        return shardId.value() % (dsCount * tableCount) % tableCount;
    }
}
