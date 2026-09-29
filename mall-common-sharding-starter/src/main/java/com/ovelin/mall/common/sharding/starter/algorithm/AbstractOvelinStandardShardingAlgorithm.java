package com.ovelin.mall.common.sharding.starter.algorithm;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.Properties;

abstract class AbstractOvelinStandardShardingAlgorithm implements StandardShardingAlgorithm<Long> {
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
    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Long> shardingValue) {;
        long shardedId = shardingValue.getValue();
        int shardId = ShardedId.shardOf(shardedId);
        long tableIndex = doSharding(shardId);
        String tableIndexStr = String.valueOf(tableIndex);
        for (String target : availableTargetNames) {
            if (target.endsWith(tableIndexStr)) {
                return target;
            }
        }
        throw new IllegalStateException("No matching table for id=" + shardedId);
    }

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, RangeShardingValue<Long> shardingValue) {
        return availableTargetNames;
    }
}
