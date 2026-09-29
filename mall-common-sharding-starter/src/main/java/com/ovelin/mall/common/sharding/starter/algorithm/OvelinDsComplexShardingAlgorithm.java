package com.ovelin.mall.common.sharding.starter.algorithm;

public class OvelinDsComplexShardingAlgorithm extends AbstractOvelinComplexShardingAlgorithm {
    @Override
    protected long doSharding(long shardId) {
        return ShardCalculator.shardingDsIndex(shardId, dsCount, tableCount);
    }

    @Override
    public String getType() {
        return "OVELIN_DS_COMPLEX_SHARDING";
    }
}
