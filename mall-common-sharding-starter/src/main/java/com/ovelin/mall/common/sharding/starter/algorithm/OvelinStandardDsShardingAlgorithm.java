package com.ovelin.mall.common.sharding.starter.algorithm;

import com.ovelin.mall.common.sharding.core.ov.ShardId;
/**
 * 按单个字段分片计算数据源的标准分片方式；
 * 不校验字段名字，支持String和ShardedId(Long)两种类型
 */
public class OvelinStandardDsShardingAlgorithm extends AbstractOvelinStandardShardingAlgorithm {
    @Override
    protected int resolveIndex(ShardId shardId) {
        return dataSourceIndexResolver.resolve(shardId, dsCount, tableCount);
    }

    @Override
    public String getType() {
        return "OVELIN_STANDARD_DS_SHARDING";
    }
}