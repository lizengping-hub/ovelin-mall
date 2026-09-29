package com.ovelin.mall.common.sharding.starter.algorithm;

public class OvelinTableComplexShardingAlgorithm extends AbstractOvelinComplexShardingAlgorithm {
    @Override
    protected long doSharding(long shardId) {
        return ShardCalculator.shardingTableIndex(shardId, dsCount, tableCount);
    }

    @Override
    public String getType() {
        return "OVELIN_TABLE_COMPLEX_SHARDING";
    }
}
