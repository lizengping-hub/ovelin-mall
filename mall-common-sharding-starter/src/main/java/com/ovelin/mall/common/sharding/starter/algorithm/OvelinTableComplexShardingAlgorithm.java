package com.ovelin.mall.common.sharding.starter.algorithm;

import com.ovelin.mall.common.sharding.core.core.HashShardResolver;
import com.ovelin.mall.common.sharding.core.ov.ShardId;

public class OvelinTableComplexShardingAlgorithm extends AbstractOvelinComplexShardingAlgorithm {
    @Override
    protected int resolveIndex(ShardId shardId) {
        return tableIndexResolver.resolve(shardId, dsCount, tableCount);
    }

    @Override
    public String getType() {
        return "OVELIN_TABLE_COMPLEX_SHARDING";
    }
}
