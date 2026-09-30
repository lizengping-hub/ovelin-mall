package com.ovelin.mall.common.sharding.starter.algorithm;

import com.google.common.collect.Range;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OvelinTableComplexShardingAlgorithmTest {
    private final static OvelinTableComplexShardingAlgorithm OVELIN_TABLE_COMPLEX_SHARDING_ALGORITHM_1 = new OvelinTableComplexShardingAlgorithm();
    private final static Collection<String> AVAILABLE_TARGET_TABLE_NAMES_1 = new HashSet<>();

    private final static OvelinTableComplexShardingAlgorithm OVELIN_TABLE_COMPLEX_SHARDING_ALGORITHM_2 = new OvelinTableComplexShardingAlgorithm();
    private final static Collection<String> AVAILABLE_TARGET_TABLE_NAMES_2 = new HashSet<>();
    @BeforeAll
    static void setUp() {
        Properties properties = new Properties();
        properties.setProperty("ds-count", "2");;
        properties.setProperty("table-count", "2");
        OVELIN_TABLE_COMPLEX_SHARDING_ALGORITHM_1.init(properties);
        AVAILABLE_TARGET_TABLE_NAMES_1.add("table0");
        AVAILABLE_TARGET_TABLE_NAMES_1.add("table1");

        properties = new Properties();
        properties.setProperty("ds-count", "4");;
        properties.setProperty("table-count", "2");
        OVELIN_TABLE_COMPLEX_SHARDING_ALGORITHM_2.init(properties);
        AVAILABLE_TARGET_TABLE_NAMES_2.add("table0");
        AVAILABLE_TARGET_TABLE_NAMES_2.add("table1");

    }
    @Test
    void shouldShardingShardedIdConfig1(){
        shouldShardingShardedId(OVELIN_TABLE_COMPLEX_SHARDING_ALGORITHM_1, AVAILABLE_TARGET_TABLE_NAMES_1, ShardedId.of(0, System.currentTimeMillis()), "table0");
        shouldShardingShardedId(OVELIN_TABLE_COMPLEX_SHARDING_ALGORITHM_1, AVAILABLE_TARGET_TABLE_NAMES_1, ShardedId.of(1, System.currentTimeMillis()), "table1");
        shouldShardingShardedId(OVELIN_TABLE_COMPLEX_SHARDING_ALGORITHM_1, AVAILABLE_TARGET_TABLE_NAMES_1, ShardedId.of(2, System.currentTimeMillis()), "table0");
        shouldShardingShardedId(OVELIN_TABLE_COMPLEX_SHARDING_ALGORITHM_1, AVAILABLE_TARGET_TABLE_NAMES_1, ShardedId.of(3, System.currentTimeMillis()), "table1");
        shouldShardingShardedId(OVELIN_TABLE_COMPLEX_SHARDING_ALGORITHM_1, AVAILABLE_TARGET_TABLE_NAMES_1, ShardedId.of(4, System.currentTimeMillis()), "table0");
        shouldShardingShardedId(OVELIN_TABLE_COMPLEX_SHARDING_ALGORITHM_1, AVAILABLE_TARGET_TABLE_NAMES_1, ShardedId.of(5, System.currentTimeMillis()), "table1");
        shouldShardingShardedId(OVELIN_TABLE_COMPLEX_SHARDING_ALGORITHM_1, AVAILABLE_TARGET_TABLE_NAMES_1, ShardedId.of(6, System.currentTimeMillis()), "table0");
        shouldShardingShardedId(OVELIN_TABLE_COMPLEX_SHARDING_ALGORITHM_1, AVAILABLE_TARGET_TABLE_NAMES_1, ShardedId.of(7, System.currentTimeMillis()), "table1");
    }
    @Test
    void shouldShardingShardedIdConfig2(){
        shouldShardingShardedId(OVELIN_TABLE_COMPLEX_SHARDING_ALGORITHM_2, AVAILABLE_TARGET_TABLE_NAMES_2, ShardedId.of(0, System.currentTimeMillis()), "table0");
        shouldShardingShardedId(OVELIN_TABLE_COMPLEX_SHARDING_ALGORITHM_2, AVAILABLE_TARGET_TABLE_NAMES_2, ShardedId.of(1, System.currentTimeMillis()), "table1");
        shouldShardingShardedId(OVELIN_TABLE_COMPLEX_SHARDING_ALGORITHM_2, AVAILABLE_TARGET_TABLE_NAMES_2, ShardedId.of(2, System.currentTimeMillis()), "table0");
        shouldShardingShardedId(OVELIN_TABLE_COMPLEX_SHARDING_ALGORITHM_2, AVAILABLE_TARGET_TABLE_NAMES_2, ShardedId.of(3, System.currentTimeMillis()), "table1");
        shouldShardingShardedId(OVELIN_TABLE_COMPLEX_SHARDING_ALGORITHM_2, AVAILABLE_TARGET_TABLE_NAMES_2, ShardedId.of(4, System.currentTimeMillis()), "table0");
        shouldShardingShardedId(OVELIN_TABLE_COMPLEX_SHARDING_ALGORITHM_2, AVAILABLE_TARGET_TABLE_NAMES_2, ShardedId.of(5, System.currentTimeMillis()), "table1");
        shouldShardingShardedId(OVELIN_TABLE_COMPLEX_SHARDING_ALGORITHM_2, AVAILABLE_TARGET_TABLE_NAMES_2, ShardedId.of(6, System.currentTimeMillis()), "table0");
        shouldShardingShardedId(OVELIN_TABLE_COMPLEX_SHARDING_ALGORITHM_2, AVAILABLE_TARGET_TABLE_NAMES_2, ShardedId.of(7, System.currentTimeMillis()), "table1");
    }

    List<Map.Entry<Integer, String>> getEntriesForShardingKeys(int shardCount){
        HashMap<Integer, String> shardIdMap = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            String shardingKey = "user:" + i;
            int shardId = ShardCalculator.shardIdFor(shardingKey);
            if (shardId % shardCount ==0){
                shardIdMap.put(shardId, shardingKey);
            }
            if (shardIdMap.size() == shardCount){
                break;
            }
        }
        if (shardIdMap.size() < shardCount){
            throw new IllegalStateException("Not enough sharding keys found for the given shard count");
        }
        return List.copyOf(shardIdMap.entrySet()).stream().sorted(Comparator.comparingInt(Map.Entry::getKey)).toList();
    }
    @Test
    void shouldShardingStringFieldConfig1(){
        List<Map.Entry<Integer, String>> shardingKeys = getEntriesForShardingKeys(8);
        shardingKeys.forEach(System.out::println);
        for (int i = 0; i < 8; i++) {
            Map.Entry<Integer, String> entry = shardingKeys.get(i);
            shouldShardingShardedId(OVELIN_TABLE_COMPLEX_SHARDING_ALGORITHM_1, AVAILABLE_TARGET_TABLE_NAMES_1, entry.getKey(), entry.getValue(), "table" + (entry.getKey() % (4 * 2) % 2));
        }
    }

    @Test
    void shouldShardingStringFieldConfig2(){
        List<Map.Entry<Integer, String>> shardingKeys = getEntriesForShardingKeys(8);
        shardingKeys.forEach(System.out::println);
        for (int i = 0; i < 8; i++) {
            Map.Entry<Integer, String> entry = shardingKeys.get(i);
            shouldShardingShardedId(OVELIN_TABLE_COMPLEX_SHARDING_ALGORITHM_1, AVAILABLE_TARGET_TABLE_NAMES_1, entry.getKey(), entry.getValue(), "table" + (entry.getKey() % (2 * 2) % 2));
        }
    }

    void shouldShardingShardedId(OvelinTableComplexShardingAlgorithm ovelinTableComplexShardingAlgorithm, Collection<String> availableTargetDsNames, int shardId, String shardingKey, String ds) {
        HashMap<String, Collection<Comparable<?>>> columnNameAndShardingValuesMap = new HashMap<>();
        columnNameAndShardingValuesMap.put("k", new HashSet<>(List.of(shardingKey)));
        columnNameAndShardingValuesMap.put("k2", new HashSet<>(List.of(ShardedId.of(shardId, System.currentTimeMillis()).value())));
        Map<String, Range<Comparable<?>>> columnNameAndRangeValuesMap = new HashMap<>();
        ComplexKeysShardingValue<Comparable<?>> shardingValue = new ComplexKeysShardingValue<>("table", columnNameAndShardingValuesMap, columnNameAndRangeValuesMap);
        Collection<String> resultDsNames = ovelinTableComplexShardingAlgorithm.doSharding(availableTargetDsNames, shardingValue);
        assertEquals(Set.of(ds), resultDsNames);
    }

    void shouldShardingShardedId(OvelinTableComplexShardingAlgorithm ovelinTableComplexShardingAlgorithm, Collection<String> availableTargetDsNames, ShardedId shardedId, String ds) {
        HashMap<String, Collection<Comparable<?>>> columnNameAndShardingValuesMap = new HashMap<>();
        columnNameAndShardingValuesMap.put("shard_id", new HashSet<>(List.of(shardedId.value())));
        Map<String, Range<Comparable<?>>> columnNameAndRangeValuesMap = new HashMap<>();
        ComplexKeysShardingValue<Comparable<?>> shardingValue = new ComplexKeysShardingValue<>("table", columnNameAndShardingValuesMap, columnNameAndRangeValuesMap);
        Collection<String> resultDsNames = ovelinTableComplexShardingAlgorithm.doSharding(availableTargetDsNames, shardingValue);
        assertEquals(Set.of(ds), resultDsNames);
    }
}
