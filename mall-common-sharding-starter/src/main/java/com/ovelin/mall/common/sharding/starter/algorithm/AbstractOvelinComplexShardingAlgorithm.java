package com.ovelin.mall.common.sharding.starter.algorithm;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;

import java.util.*;
import java.util.stream.Collectors;

abstract class AbstractOvelinComplexShardingAlgorithm implements ComplexKeysShardingAlgorithm<Comparable<?>> {
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

    long doSharding(Comparable<?> shardKeyObj) {
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

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, ComplexKeysShardingValue<Comparable<?>> shardingValue) {
        if (shardingValue.getColumnNameAndRangeValuesMap() != null && !shardingValue.getColumnNameAndRangeValuesMap().isEmpty()) {
            return availableTargetNames; // 范围查询暂不优化，全库扫
        }

        Map<String, Collection<Comparable<?>>> values =
                shardingValue.getColumnNameAndShardingValuesMap();
        Collection<Comparable<?>> idValues = null;
        for (Collection<Comparable<?>> valueCollection : values.values()) {
            if (valueCollection != null && !valueCollection.isEmpty()) {
                idValues = valueCollection;
                break;
            }
        }
        if (idValues == null) {
            return availableTargetNames;
        }
        if (idValues.size() == 1) {
            Comparable<?> idObject = idValues.iterator().next();
            long dbIndex = doSharding(idObject);
            return availableTargetNames.stream().filter(target -> target.endsWith(String.valueOf(dbIndex))).collect(Collectors.toSet());
        } else {
            Set<Long> dbIndexes = idValues.stream().map(this::doSharding).collect(Collectors.toSet());
            return availableTargetNames.stream().filter(target -> {
                for (Long dbIndex : dbIndexes) {
                    if (target.endsWith(String.valueOf(dbIndex))) {
                        return true;
                    }
                }
                return false;
            }).collect(Collectors.toSet());
        }
    }
}
