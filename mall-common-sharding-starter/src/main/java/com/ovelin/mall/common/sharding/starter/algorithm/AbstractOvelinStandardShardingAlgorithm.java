package com.ovelin.mall.common.sharding.starter.algorithm;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;

abstract class AbstractOvelinStandardShardingAlgorithm extends AbstractOvelinShardingAlgorithm<Comparable<?>> implements StandardShardingAlgorithm<Comparable<?>> {


    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Comparable<?>> shardingValue) {;
        Comparable<?> shardedId = shardingValue.getValue();
        int resolvedIndex = resolveIndex(shardedId);
        String tableIndexStr = String.valueOf(resolvedIndex);
        for (String target : availableTargetNames) {
            if (target.endsWith(tableIndexStr)) {
                return target;
            }
        }
        throw new IllegalStateException("No matching table for id=" + shardedId);
    }

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, RangeShardingValue<Comparable<?>> shardingValue) {
        return availableTargetNames;
    }
}
