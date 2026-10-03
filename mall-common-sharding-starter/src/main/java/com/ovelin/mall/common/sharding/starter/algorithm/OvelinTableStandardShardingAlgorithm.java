package com.ovelin.mall.common.sharding.starter.algorithm;

import com.ovelin.mall.common.sharding.core.core.HashShardResolver;
import com.ovelin.mall.common.sharding.core.ov.ShardId;

public class OvelinTableStandardShardingAlgorithm extends AbstractOvelinStandardShardingAlgorithm {
    @Override
    protected int resolveIndex(ShardId shardId) {
        return tableIndexResolver.resolve(shardId, dsCount, tableCount);
    }

    @Override
    public String getType() {
        return "OVELIN_TABLE_STANDARD_SHARDING";
    }
}