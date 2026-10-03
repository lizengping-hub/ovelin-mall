package com.ovelin.mall.common.sharding.starter.algorithm;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.common.sharding.starter.utils.SequenceShardKeyResolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OvelinTableComplexShardingAlgorithmTest extends AbstractComplexShardingAlgorithmTest{
    private final static OvelinTableComplexShardingAlgorithm ALGORITHM_2_2 = new OvelinTableComplexShardingAlgorithm();
    private final static OvelinTableComplexShardingAlgorithm ALGORITHM_4_2 = new OvelinTableComplexShardingAlgorithm();
    private final static OvelinTableComplexShardingAlgorithm ALGORITHM_4_4 = new OvelinTableComplexShardingAlgorithm();

    @BeforeAll
    static void setUp() {

        ALGORITHM_2_2.init(CONFIG_2_2.properties());
        ALGORITHM_4_2.init(CONFIG_4_2.properties());
        ALGORITHM_4_4.init(CONFIG_4_4.properties());


    }
    @Test
    void shouldShardingShardedIdConfig_2_2(){
        shouldShardingShardedId(ALGORITHM_2_2, CONFIG_2_2, ShardedId.of(0, System.currentTimeMillis()), "table0");
        shouldShardingShardedId(ALGORITHM_2_2, CONFIG_2_2, ShardedId.of(1, System.currentTimeMillis()), "table1");
        shouldShardingShardedId(ALGORITHM_2_2, CONFIG_2_2, ShardedId.of(2, System.currentTimeMillis()), "table0");
        shouldShardingShardedId(ALGORITHM_2_2, CONFIG_2_2, ShardedId.of(3, System.currentTimeMillis()), "table1");
        shouldShardingShardedId(ALGORITHM_2_2, CONFIG_2_2, ShardedId.of(4, System.currentTimeMillis()), "table0");
        shouldShardingShardedId(ALGORITHM_2_2, CONFIG_2_2, ShardedId.of(5, System.currentTimeMillis()), "table1");
        shouldShardingShardedId(ALGORITHM_2_2, CONFIG_2_2, ShardedId.of(6, System.currentTimeMillis()), "table0");
        shouldShardingShardedId(ALGORITHM_2_2, CONFIG_2_2, ShardedId.of(7, System.currentTimeMillis()), "table1");
    }
    @Test
    void shouldShardingShardedIdConfig_4_2(){
        shouldShardingShardedId(ALGORITHM_4_2, CONFIG_4_2, ShardedId.of(0, System.currentTimeMillis()), "table0");
        shouldShardingShardedId(ALGORITHM_4_2, CONFIG_4_2, ShardedId.of(1, System.currentTimeMillis()), "table1");
        shouldShardingShardedId(ALGORITHM_4_2, CONFIG_4_2, ShardedId.of(2, System.currentTimeMillis()), "table0");
        shouldShardingShardedId(ALGORITHM_4_2, CONFIG_4_2, ShardedId.of(3, System.currentTimeMillis()), "table1");
        shouldShardingShardedId(ALGORITHM_4_2, CONFIG_4_2, ShardedId.of(4, System.currentTimeMillis()), "table0");
        shouldShardingShardedId(ALGORITHM_4_2, CONFIG_4_2, ShardedId.of(5, System.currentTimeMillis()), "table1");
        shouldShardingShardedId(ALGORITHM_4_2, CONFIG_4_2, ShardedId.of(6, System.currentTimeMillis()), "table0");
        shouldShardingShardedId(ALGORITHM_4_2, CONFIG_4_2, ShardedId.of(7, System.currentTimeMillis()), "table1");
    }
    @Test
    void shouldShardingShardedIdConfig_4_4(){
        shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(0, System.currentTimeMillis()), "table0");
        shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(1, System.currentTimeMillis()), "table1");
        shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(2, System.currentTimeMillis()), "table2");
        shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(3, System.currentTimeMillis()), "table3");
        shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(4, System.currentTimeMillis()), "table0");
        shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(5, System.currentTimeMillis()), "table1");
        shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(6, System.currentTimeMillis()), "table2");
        shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(7, System.currentTimeMillis()), "table3");
    }


    @Test
    void shouldShardingStringFieldConfig_2_2(){
        List<SequenceShardKeyResolver.ShardKeyRecord> shardKeys = SequenceShardKeyResolver.getSequenceShardKeys(4);
        shouldShardingStringKey(shardKeys.get(0), ALGORITHM_2_2, CONFIG_2_2,  "table0");
        shouldShardingStringKey(shardKeys.get(1), ALGORITHM_2_2, CONFIG_2_2,  "table1");
        shouldShardingStringKey(shardKeys.get(2), ALGORITHM_2_2, CONFIG_2_2,  "table0");
        shouldShardingStringKey(shardKeys.get(3), ALGORITHM_2_2, CONFIG_2_2,  "table1");
    }
    @Test
    void shouldShardingStringFieldConfig_4_2(){
        List<SequenceShardKeyResolver.ShardKeyRecord> shardKeys = SequenceShardKeyResolver.getSequenceShardKeys(8);
        shouldShardingStringKey(shardKeys.get(0), ALGORITHM_4_2, CONFIG_4_2,  "table0");
        shouldShardingStringKey(shardKeys.get(1), ALGORITHM_4_2, CONFIG_4_2,  "table1");
        shouldShardingStringKey(shardKeys.get(2), ALGORITHM_4_2, CONFIG_4_2,  "table0");
        shouldShardingStringKey(shardKeys.get(3), ALGORITHM_4_2, CONFIG_4_2,  "table1");
        shouldShardingStringKey(shardKeys.get(4), ALGORITHM_4_2, CONFIG_4_2,  "table0");
        shouldShardingStringKey(shardKeys.get(5), ALGORITHM_4_2, CONFIG_4_2,  "table1");
        shouldShardingStringKey(shardKeys.get(6), ALGORITHM_4_2, CONFIG_4_2,  "table0");
        shouldShardingStringKey(shardKeys.get(7), ALGORITHM_4_2, CONFIG_4_2,  "table1");
    }
    @Test
    void shouldShardingStringFieldConfig_4_4(){
        List<SequenceShardKeyResolver.ShardKeyRecord> shardKeys = SequenceShardKeyResolver.getSequenceShardKeys(16);
        shouldShardingStringKey(shardKeys.get(0), ALGORITHM_4_4, CONFIG_4_4,  "table0");
        shouldShardingStringKey(shardKeys.get(1), ALGORITHM_4_4, CONFIG_4_4,  "table1");
        shouldShardingStringKey(shardKeys.get(2), ALGORITHM_4_4, CONFIG_4_4,  "table2");
        shouldShardingStringKey(shardKeys.get(3), ALGORITHM_4_4, CONFIG_4_4,  "table3");
        shouldShardingStringKey(shardKeys.get(4), ALGORITHM_4_4, CONFIG_4_4,  "table0");
        shouldShardingStringKey(shardKeys.get(5), ALGORITHM_4_4, CONFIG_4_4,  "table1");
        shouldShardingStringKey(shardKeys.get(6), ALGORITHM_4_4, CONFIG_4_4,  "table2");
        shouldShardingStringKey(shardKeys.get(7), ALGORITHM_4_4, CONFIG_4_4,  "table3");
    }

    void shouldShardingStringKey(SequenceShardKeyResolver.ShardKeyRecord shardKey, OvelinTableComplexShardingAlgorithm ovelinDsComplexShardingAlgorithm, Config config, String ds) {
        super.shouldShardingStringKey(shardKey, ovelinDsComplexShardingAlgorithm, config.tableNames(), ds);
    }
    void shouldShardingShardedId(OvelinTableComplexShardingAlgorithm ovelinTableComplexShardingAlgorithm, Config config,ShardedId shardedId, String shardingKey, String table) {
        super.shouldShardingShardedId(ovelinTableComplexShardingAlgorithm, config.tableNames(), shardedId, shardingKey, table);
    }
    void shouldShardingShardedId(OvelinTableComplexShardingAlgorithm ovelinTableComplexShardingAlgorithm, Config config, String shardingKey, String table) {
        super.shouldShardingShardedId(ovelinTableComplexShardingAlgorithm, config.tableNames(), shardingKey, table);
    }

    void shouldShardingShardedId(OvelinTableComplexShardingAlgorithm ovelinTableComplexShardingAlgorithm, Config config, ShardedId shardedId, String table) {
        super.shouldShardingShardedId(ovelinTableComplexShardingAlgorithm, config.tableNames(), shardedId, table);
    }
}
