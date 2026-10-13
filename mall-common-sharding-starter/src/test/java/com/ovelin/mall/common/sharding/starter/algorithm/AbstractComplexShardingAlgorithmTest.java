package com.ovelin.mall.common.sharding.starter.algorithm;

import com.google.common.collect.Range;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.common.sharding.starter.utils.SequenceShardKeyResolver;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AbstractComplexShardingAlgorithmTest{
    void shouldShardingStringKey(SequenceShardKeyResolver.ShardKey2Record shardKey, AbstractOvelinComplexShardingAlgorithm ovelinDsComplexShardingAlgorithm, Set<String> availableTargetNames, String targetName) {
        ShardedId shardedId = ShardedId.of(shardKey.shardId(), System.currentTimeMillis());
        shouldShardingShardedId(ovelinDsComplexShardingAlgorithm, availableTargetNames, targetName, shardedId, null, null);
        shouldShardingShardedId(ovelinDsComplexShardingAlgorithm, availableTargetNames, targetName, null, shardKey.shardKey0(), shardKey.shardKey1());
        shouldShardingShardedId(ovelinDsComplexShardingAlgorithm, availableTargetNames, targetName, shardedId, shardKey.shardKey0(), shardKey.shardKey1());
    }
    void shouldShardingStringKey(SequenceShardKeyResolver.ShardKeyRecord shardKey, AbstractOvelinComplexShardingAlgorithm ovelinDsComplexShardingAlgorithm, Set<String> availableTargetNames, String targetName) {
        ShardedId shardedId = ShardedId.of(shardKey.shardId(), System.currentTimeMillis());
        shouldShardingShardedId(ovelinDsComplexShardingAlgorithm, availableTargetNames, targetName, shardedId, null, null);
        shouldShardingShardedId(ovelinDsComplexShardingAlgorithm, availableTargetNames, targetName, null, shardKey.shardKey(), null);
        shouldShardingShardedId(ovelinDsComplexShardingAlgorithm, availableTargetNames, targetName, shardedId, shardKey.shardKey(), null);
    }
    void shouldShardingShardedId(AbstractOvelinComplexShardingAlgorithm ovelinDsComplexShardingAlgorithm, Set<String> availableTargetNames, String targetName, ShardedId shardedId, String shardingKey0, String shardingKey1) {
        HashMap<String, Collection<Comparable<?>>> columnNameAndShardingValuesMap = new HashMap<>();
        if (shardedId != null){
            columnNameAndShardingValuesMap.put("k2", new HashSet<>(List.of(shardedId.value())));
        }
        if (shardingKey0 != null){
            columnNameAndShardingValuesMap.put("identity_type", new HashSet<>(List.of(shardingKey0)));
        }
        if (shardingKey1 != null){
            columnNameAndShardingValuesMap.put("identifier", new HashSet<>(List.of(shardingKey1)));
        }

        Map<String, Range<Comparable<?>>> columnNameAndRangeValuesMap = new HashMap<>();
        ComplexKeysShardingValue<Comparable<?>> shardingValue = new ComplexKeysShardingValue<>("table", columnNameAndShardingValuesMap, columnNameAndRangeValuesMap);
        Collection<String> resultDsNames = ovelinDsComplexShardingAlgorithm.doSharding(availableTargetNames, shardingValue);
        assertEquals(Set.of(targetName), resultDsNames);
    }

}
