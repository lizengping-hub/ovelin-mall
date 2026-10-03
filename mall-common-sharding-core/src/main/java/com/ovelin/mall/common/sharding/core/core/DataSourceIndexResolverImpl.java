package com.ovelin.mall.common.sharding.core.core;

import com.ovelin.mall.common.sharding.core.api.DataSourceIndexResolver;
import com.ovelin.mall.common.sharding.core.ov.ShardId;

public class DataSourceIndexResolverImpl implements DataSourceIndexResolver {
    @Override
    public int resolve(ShardId shardId, int dsCount, int tableCount) {
        // Implementation for resolving data source index
        return  (shardId.value() % (dsCount * tableCount) / tableCount);
    }
}
