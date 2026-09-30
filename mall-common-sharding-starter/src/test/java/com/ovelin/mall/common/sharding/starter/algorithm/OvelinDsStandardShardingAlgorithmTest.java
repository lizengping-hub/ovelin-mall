package com.ovelin.mall.common.sharding.starter.algorithm;

import com.google.common.collect.Range;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import org.apache.shardingsphere.infra.datanode.DataNodeInfo;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OvelinDsStandardShardingAlgorithmTest {
    private final static OvelinDsStandardShardingAlgorithm OVELIN_DS_COMPLEX_SHARDING_ALGORITHM_1 = new OvelinDsStandardShardingAlgorithm();
    private final static Collection<String> AVAILABLE_TARGET_DS_NAMES_1 = new HashSet<>();

    private final static OvelinDsStandardShardingAlgorithm OVELIN_DS_COMPLEX_SHARDING_ALGORITHM_2 = new OvelinDsStandardShardingAlgorithm();
    private final static Collection<String> AVAILABLE_TARGET_DS_NAMES_2 = new HashSet<>();
    @BeforeAll
    static void setUp() {
        Properties properties = new Properties();
        properties.setProperty("ds-count", "2");;
        properties.setProperty("table-count", "2");
        OVELIN_DS_COMPLEX_SHARDING_ALGORITHM_1.init(properties);
        AVAILABLE_TARGET_DS_NAMES_1.add("ds0");
        AVAILABLE_TARGET_DS_NAMES_1.add("ds1");

        properties = new Properties();
        properties.setProperty("ds-count", "4");;
        properties.setProperty("table-count", "2");
        OVELIN_DS_COMPLEX_SHARDING_ALGORITHM_2.init(properties);
        AVAILABLE_TARGET_DS_NAMES_2.add("ds0");
        AVAILABLE_TARGET_DS_NAMES_2.add("ds1");
        AVAILABLE_TARGET_DS_NAMES_2.add("ds2");
        AVAILABLE_TARGET_DS_NAMES_2.add("ds3");

    }
    @Test
    void shouldShardingShardedIdConfig1(){
        shouldShardingShardedId(OVELIN_DS_COMPLEX_SHARDING_ALGORITHM_1, AVAILABLE_TARGET_DS_NAMES_1, ShardedId.of(0, System.currentTimeMillis()), "ds0");
        shouldShardingShardedId(OVELIN_DS_COMPLEX_SHARDING_ALGORITHM_1, AVAILABLE_TARGET_DS_NAMES_1, ShardedId.of(1, System.currentTimeMillis()), "ds0");
        shouldShardingShardedId(OVELIN_DS_COMPLEX_SHARDING_ALGORITHM_1, AVAILABLE_TARGET_DS_NAMES_1, ShardedId.of(2, System.currentTimeMillis()), "ds1");
        shouldShardingShardedId(OVELIN_DS_COMPLEX_SHARDING_ALGORITHM_1, AVAILABLE_TARGET_DS_NAMES_1, ShardedId.of(3, System.currentTimeMillis()), "ds1");
        shouldShardingShardedId(OVELIN_DS_COMPLEX_SHARDING_ALGORITHM_1, AVAILABLE_TARGET_DS_NAMES_1, ShardedId.of(4, System.currentTimeMillis()), "ds0");
        shouldShardingShardedId(OVELIN_DS_COMPLEX_SHARDING_ALGORITHM_1, AVAILABLE_TARGET_DS_NAMES_1, ShardedId.of(5, System.currentTimeMillis()), "ds0");
        shouldShardingShardedId(OVELIN_DS_COMPLEX_SHARDING_ALGORITHM_1, AVAILABLE_TARGET_DS_NAMES_1, ShardedId.of(6, System.currentTimeMillis()), "ds1");
        shouldShardingShardedId(OVELIN_DS_COMPLEX_SHARDING_ALGORITHM_1, AVAILABLE_TARGET_DS_NAMES_1, ShardedId.of(7, System.currentTimeMillis()), "ds1");
    }
    @Test
    void shouldShardingShardedIdConfig2(){
        shouldShardingShardedId(OVELIN_DS_COMPLEX_SHARDING_ALGORITHM_2, AVAILABLE_TARGET_DS_NAMES_2, ShardedId.of(0, System.currentTimeMillis()), "ds0");
        shouldShardingShardedId(OVELIN_DS_COMPLEX_SHARDING_ALGORITHM_2, AVAILABLE_TARGET_DS_NAMES_2, ShardedId.of(1, System.currentTimeMillis()), "ds0");
        shouldShardingShardedId(OVELIN_DS_COMPLEX_SHARDING_ALGORITHM_2, AVAILABLE_TARGET_DS_NAMES_2, ShardedId.of(2, System.currentTimeMillis()), "ds1");
        shouldShardingShardedId(OVELIN_DS_COMPLEX_SHARDING_ALGORITHM_2, AVAILABLE_TARGET_DS_NAMES_2, ShardedId.of(3, System.currentTimeMillis()), "ds1");
        shouldShardingShardedId(OVELIN_DS_COMPLEX_SHARDING_ALGORITHM_2, AVAILABLE_TARGET_DS_NAMES_2, ShardedId.of(4, System.currentTimeMillis()), "ds2");
        shouldShardingShardedId(OVELIN_DS_COMPLEX_SHARDING_ALGORITHM_2, AVAILABLE_TARGET_DS_NAMES_2, ShardedId.of(5, System.currentTimeMillis()), "ds2");
        shouldShardingShardedId(OVELIN_DS_COMPLEX_SHARDING_ALGORITHM_2, AVAILABLE_TARGET_DS_NAMES_2, ShardedId.of(6, System.currentTimeMillis()), "ds3");
        shouldShardingShardedId(OVELIN_DS_COMPLEX_SHARDING_ALGORITHM_2, AVAILABLE_TARGET_DS_NAMES_2, ShardedId.of(7, System.currentTimeMillis()), "ds3");
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
            shouldShardingShardedId(OVELIN_DS_COMPLEX_SHARDING_ALGORITHM_1, AVAILABLE_TARGET_DS_NAMES_1, entry.getValue(), "ds" + (entry.getKey() % (4 * 2) / 2));
        }
    }

    @Test
    void shouldShardingStringFieldConfig2(){
        List<Map.Entry<Integer, String>> shardingKeys = getEntriesForShardingKeys(8);
        shardingKeys.forEach(System.out::println);
        for (int i = 0; i < 8; i++) {
            Map.Entry<Integer, String> entry = shardingKeys.get(i);
            shouldShardingShardedId(OVELIN_DS_COMPLEX_SHARDING_ALGORITHM_1, AVAILABLE_TARGET_DS_NAMES_1, entry.getValue(), "ds" + (entry.getKey() % (2 * 2) / 2));
        }
    }

    void shouldShardingShardedId(OvelinDsStandardShardingAlgorithm ovelinDsComplexShardingAlgorithm, Collection<String> availableTargetDsNames, String shardingKey, String ds) {

        DataNodeInfo dataNodeInfo = new DataNodeInfo("table", 1, 'g');
        PreciseShardingValue<Comparable<?>> preciseShardingValue = new PreciseShardingValue<>("table", "k2", dataNodeInfo, shardingKey);

        String resultDsName = ovelinDsComplexShardingAlgorithm.doSharding(availableTargetDsNames, preciseShardingValue);
        assertEquals(ds, resultDsName);
    }

    void shouldShardingShardedId(OvelinDsStandardShardingAlgorithm ovelinDsComplexShardingAlgorithm, Collection<String> availableTargetDsNames, ShardedId shardedId, String ds) {
        DataNodeInfo dataNodeInfo = new DataNodeInfo("table", 1, 'g');
        PreciseShardingValue<Comparable<?>> preciseShardingValue = new PreciseShardingValue<>("table", "k2", dataNodeInfo,
                shardedId.value());

        String resultDsName = ovelinDsComplexShardingAlgorithm.doSharding(availableTargetDsNames, preciseShardingValue);
        assertEquals(ds, resultDsName);
    }
}
