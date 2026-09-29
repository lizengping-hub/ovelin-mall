package com.ovelin.mall.common.sharding.starter.algorithm;

public class OvelinTableStandardShardingAlgorithm extends AbstractOvelinStandardShardingAlgorithm {
    @Override
    protected long doSharding(long shardId) {
        return ShardCalculator.shardingTableIndex(shardId, dsCount, tableCount);
    }

    @Override
    public String getType() {
        return "OVELIN_TABLE_STANDARD_SHARDING";
    }
}