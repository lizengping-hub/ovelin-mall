package com.ovelin.mall.common.sharding.starter.algorithm;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.common.sharding.starter.utils.SequenceShardKeyResolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.ovelin.mall.common.sharding.starter.algorithm.ShardingAlgorithmTestConfig.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OvelinComplexTableShardingAlgorithmTest extends AbstractComplexShardingAlgorithmTest{
    private final static OvelinComplexTableShardingAlgorithm ALGORITHM_2_2 = new OvelinComplexTableShardingAlgorithm();
    private final static OvelinComplexTableShardingAlgorithm ALGORITHM_4_2 = new OvelinComplexTableShardingAlgorithm();
    private final static OvelinComplexTableShardingAlgorithm ALGORITHM_4_4 = new OvelinComplexTableShardingAlgorithm();

    @BeforeAll
    static void setUp() {

        ALGORITHM_2_2.init(CONFIG_2_2.properties());
        ALGORITHM_4_2.init(CONFIG_4_2.properties());
        ALGORITHM_4_4.init(CONFIG_4_4.properties());


    }
    @Test
    void shouldShardingShardedIdConfig_2_2(){
        String[] tables = {"table0","table1", "table0", "table1", "table0","table1", "table0", "table1"};
        for (int i = 0; i < 8; i++) {
            shouldShardingShardedId(ALGORITHM_2_2, CONFIG_2_2, ShardedId.of(i, System.currentTimeMillis()), tables[i]);
        }
    }
    @Test
    void shouldShardingShardedIdConfig_4_2(){
        String[] tables = {"table0","table1", "table0", "table1", "table0","table1", "table0", "table1"};
        for (int i = 0; i < 8; i++) {
            shouldShardingShardedId(ALGORITHM_4_2, CONFIG_4_2, ShardedId.of(i, System.currentTimeMillis()), tables[i]);
        }
    }
    @Test
    void shouldShardingShardedIdConfig_4_4(){
        String[] tables = {"table0","table1","table2","table3", "table0", "table1", "table2", "table3"};
        for (int i = 0; i < 8; i++) {
            shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(i, System.currentTimeMillis()), tables[i]);
        }
    }


    @Test
    void shouldShardingStringFieldConfig_2_2(){
        List<SequenceShardKeyResolver.ShardKeyRecord> shardKeys = SequenceShardKeyResolver.getSequenceShardKeys(4);
        String[] tables = {"table0","table1", "table0", "table1", };
        for (int i = 0; i < 4; i++) {
            shouldShardingStringKey(shardKeys.get(i), ALGORITHM_2_2, CONFIG_2_2, tables[i]);
        }
    }
    @Test
    void shouldShardingStringFieldConfig_4_2(){
        List<SequenceShardKeyResolver.ShardKeyRecord> shardKeys = SequenceShardKeyResolver.getSequenceShardKeys(8);
        String[] tables = {"table0","table1", "table0", "table1", "table0","table1", "table0", "table1"};
        for (int i = 0; i < 8; i++) {
            shouldShardingStringKey(shardKeys.get(i), ALGORITHM_4_2, CONFIG_4_2, tables[i]);
        }
    }
    @Test
    void shouldShardingStringFieldConfig_4_4(){
        List<SequenceShardKeyResolver.ShardKeyRecord> shardKeys = SequenceShardKeyResolver.getSequenceShardKeys(8);
        String[] tables = {"table0","table1","table2","table3", "table0", "table1", "table2", "table3"};
        for (int i = 0; i < 8; i++) {
            shouldShardingStringKey(shardKeys.get(i), ALGORITHM_4_4, CONFIG_4_4, tables[i]);
        }
    }

    void shouldShardingStringKey(SequenceShardKeyResolver.ShardKeyRecord shardKey, OvelinComplexTableShardingAlgorithm ovelinDsComplexShardingAlgorithm, Config config, String ds) {
        super.shouldShardingStringKey(shardKey, ovelinDsComplexShardingAlgorithm, config.tableNames(), ds);
    }

    void shouldShardingShardedId(OvelinComplexTableShardingAlgorithm ovelinTableComplexShardingAlgorithm, Config config, ShardedId shardedId, String table) {
        super.shouldShardingShardedId(ovelinTableComplexShardingAlgorithm, config.tableNames(), table, shardedId, null, null);
    }
}
