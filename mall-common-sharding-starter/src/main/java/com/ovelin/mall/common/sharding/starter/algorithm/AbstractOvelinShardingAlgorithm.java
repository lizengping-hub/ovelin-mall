package com.ovelin.mall.common.sharding.starter.algorithm;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import org.apache.shardingsphere.sharding.spi.ShardingAlgorithm;

import java.util.Properties;

abstract class AbstractOvelinShardingAlgorithm<T extends Comparable<?>> implements ShardingAlgorithm {
    protected long dsCount;
    protected long tableCount;
    @Override
    public void init(Properties props) {
        Object dsCount = props.get("ds-count");
        if (dsCount == null) {
            throw new IllegalArgumentException("dsCount is required");
        }
        Object tableCount = props.get("table-count");
        if (tableCount == null) {
            throw new IllegalArgumentException("tableCount is required");
        }
        try {
            this.dsCount = Long.parseLong(dsCount.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("dsCount must be a valid number", e);
        }
        try {
            this.tableCount = Long.parseLong(tableCount.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("tableCount must be a valid number", e);
        }
    }
    protected abstract long doSharding(long shardId);
    protected long doSharding(Comparable<?> shardKeyObj) {
        if (shardKeyObj instanceof Long shardedId) {
            int shardId = ShardedId.shardOf(shardedId);
            return doSharding(shardId);
        } else if (shardKeyObj instanceof String shardKey) {
            int shardId = ShardCalculator.shardIdFor(shardKey); // validate shardKey
            return doSharding(shardId);
        } else {
            throw new IllegalArgumentException("Unsupported id type: " + shardKeyObj.getClass().getName());
        }
    }
}
