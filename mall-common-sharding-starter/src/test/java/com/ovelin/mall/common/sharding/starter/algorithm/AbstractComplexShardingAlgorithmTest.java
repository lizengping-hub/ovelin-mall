package com.ovelin.mall.common.sharding.starter.algorithm;

import com.google.common.collect.Range;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.common.sharding.starter.utils.SequenceShardKeyResolver;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AbstractComplexShardingAlgorithmTest extends AbstractShardingAlgorithmTest{
    void shouldShardingStringKey(SequenceShardKeyResolver.ShardKeyRecord shardKey, AbstractOvelinComplexShardingAlgorithm ovelinDsComplexShardingAlgorithm, Set<String> availableTargetNames, String targetName) {
        ShardedId shardedId = ShardedId.of(shardKey.shardId(), System.currentTimeMillis());
        shouldShardingShardedId(ovelinDsComplexShardingAlgorithm, availableTargetNames, shardedId, targetName);
        shouldShardingShardedId(ovelinDsComplexShardingAlgorithm, availableTargetNames, shardKey.shardKey(), targetName);
        shouldShardingShardedId(ovelinDsComplexShardingAlgorithm, availableTargetNames, shardedId, shardKey.shardKey(), targetName);
    }
    void shouldShardingShardedId(AbstractOvelinComplexShardingAlgorithm ovelinDsComplexShardingAlgorithm, Set<String> availableTargetNames, ShardedId shardedId, String shardingKey, String targetName) {
        HashMap<String, Collection<Comparable<?>>> columnNameAndShardingValuesMap = new HashMap<>();
        columnNameAndShardingValuesMap.put("k", new HashSet<>(List.of(shardingKey)));
        columnNameAndShardingValuesMap.put("k2", new HashSet<>(List.of(shardedId.value())));
        Map<String, Range<Comparable<?>>> columnNameAndRangeValuesMap = new HashMap<>();
        ComplexKeysShardingValue<Comparable<?>> shardingValue = new ComplexKeysShardingValue<>("table", columnNameAndShardingValuesMap, columnNameAndRangeValuesMap);
        Collection<String> resultDsNames = ovelinDsComplexShardingAlgorithm.doSharding(availableTargetNames, shardingValue);
        assertEquals(Set.of(targetName), resultDsNames);
    }

    void shouldShardingShardedId(AbstractOvelinComplexShardingAlgorithm ovelinDsComplexShardingAlgorithm, Set<String> availableTargetNames, String shardingKey, String targetName) {
        HashMap<String, Collection<Comparable<?>>> columnNameAndShardingValuesMap = new HashMap<>();
        columnNameAndShardingValuesMap.put("k", new HashSet<>(List.of(shardingKey)));
        Map<String, Range<Comparable<?>>> columnNameAndRangeValuesMap = new HashMap<>();
        ComplexKeysShardingValue<Comparable<?>> shardingValue = new ComplexKeysShardingValue<>("table", columnNameAndShardingValuesMap, columnNameAndRangeValuesMap);
        Collection<String> resultDsNames = ovelinDsComplexShardingAlgorithm.doSharding(availableTargetNames, shardingValue);
        assertEquals(Set.of(targetName), resultDsNames);
    }

    void shouldShardingShardedId(AbstractOvelinComplexShardingAlgorithm ovelinDsComplexShardingAlgorithm, Set<String> availableTargetNames, ShardedId shardedId, String targetName) {
        HashMap<String, Collection<Comparable<?>>> columnNameAndShardingValuesMap = new HashMap<>();
        columnNameAndShardingValuesMap.put("shard_id", new HashSet<>(List.of(shardedId.value())));
        Map<String, Range<Comparable<?>>> columnNameAndRangeValuesMap = new HashMap<>();
        ComplexKeysShardingValue<Comparable<?>> shardingValue = new ComplexKeysShardingValue<>("table", columnNameAndShardingValuesMap, columnNameAndRangeValuesMap);
        Collection<String> resultDsNames = ovelinDsComplexShardingAlgorithm.doSharding(availableTargetNames, shardingValue);
        assertEquals(Set.of(targetName), resultDsNames);
    }
}
