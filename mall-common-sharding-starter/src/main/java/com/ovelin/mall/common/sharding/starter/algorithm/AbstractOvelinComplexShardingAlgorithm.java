package com.ovelin.mall.common.sharding.starter.algorithm;

import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;

import java.util.*;
import java.util.stream.Collectors;

abstract class AbstractOvelinComplexShardingAlgorithm extends AbstractOvelinShardingAlgorithm<Comparable<?>> implements ComplexKeysShardingAlgorithm<Comparable<?>> {

    /**
     * doSharding for data source or table name, if sharding value is range query, return all available target names
     * @param availableTargetNames available data sources or table names
     * @param shardingValue sharding value
     * @return
     */
    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, ComplexKeysShardingValue<Comparable<?>> shardingValue) {
        if (shardingValue.getColumnNameAndRangeValuesMap() != null && !shardingValue.getColumnNameAndRangeValuesMap().isEmpty()) {
            return availableTargetNames; // 范围查询暂不优化，全库扫
        }

        Map<String, Collection<Comparable<?>>> values =
                shardingValue.getColumnNameAndShardingValuesMap();
        Collection<Comparable<?>> shardKeys = null;
        for (Collection<Comparable<?>> valuesCollection : values.values()) {
            if (valuesCollection != null && !valuesCollection.isEmpty()) {
                shardKeys = valuesCollection;
                break;
            }
        }
        if (shardKeys == null) {
            return availableTargetNames;
        }
        if (shardKeys.size() == 1) {
            Comparable<?> shardKey = shardKeys.iterator().next();
            int resolvedIndex = resolveIndex(shardKey);
            return availableTargetNames.stream().filter(target -> target.endsWith(String.valueOf(resolvedIndex))).collect(Collectors.toSet());
        } else {
            Set<String> resolvedIndexes = shardKeys.stream().map(this::resolveIndex).map(String::valueOf).collect(Collectors.toSet());
            return availableTargetNames.stream().filter(target -> {
                for (String resolvedIndex : resolvedIndexes) {
                    if (target.endsWith(resolvedIndex)) {
                        return true;
                    }
                }
                return false;
            }).collect(Collectors.toSet());
        }
    }
}
