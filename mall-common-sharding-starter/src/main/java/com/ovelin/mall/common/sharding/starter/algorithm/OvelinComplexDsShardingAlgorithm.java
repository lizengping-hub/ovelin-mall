package com.ovelin.mall.common.sharding.starter.algorithm;

import com.ovelin.mall.common.sharding.core.ov.ShardId;

/**
 * 多个字段的每个字段都支持数据源的分片，这些字段的最终分片位置应该一样，有任意一个值即可完成分片;
 * 不校验字段名字，支持String和ShardedId(Long)两种类型
 */
public class OvelinComplexDsShardingAlgorithm extends AbstractOvelinComplexShardingAlgorithm {
    @Override
    protected int resolveIndex(ShardId shardId) {
        return dataSourceIndexResolver.resolve(shardId, dsCount, tableCount);
    }

    @Override
    public String getType() {
        return "OVELIN_COMPLEX_DS_SHARDING";
    }
}
