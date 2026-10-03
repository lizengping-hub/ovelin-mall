package com.ovelin.mall.common.sharding.starter.algorithm;

import com.ovelin.mall.common.sharding.core.core.HashShardResolver;
import com.ovelin.mall.common.sharding.core.ov.ShardId;

public class OvelinDsComplexShardingAlgorithm extends AbstractOvelinComplexShardingAlgorithm {
    @Override
    protected int resolveIndex(ShardId shardId) {
        return dataSourceIndexResolver.resolve(shardId, dsCount, tableCount);
    }

    @Override
    public String getType() {
        return "OVELIN_DS_COMPLEX_SHARDING";
    }
}
