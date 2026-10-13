package com.ovelin.mall.common.sharding.starter.algorithm;

import com.ovelin.mall.common.sharding.core.ov.ShardId;
/**
 * 按单个字段分片计算表的标准分片方式；
 * 不校验字段名字，支持String和ShardedId(Long)两种类型
 */
public class OvelinStandardTableShardingAlgorithm extends AbstractOvelinStandardShardingAlgorithm {
    @Override
    protected int resolveIndex(ShardId shardId) {
        return tableIndexResolver.resolve(shardId, dsCount, tableCount);
    }

    @Override
    public String getType() {
        return "OVELIN_STANDARD_TABLE_SHARDING";
    }
}