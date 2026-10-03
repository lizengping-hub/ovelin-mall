package com.ovelin.mall.common.sharding.starter.algorithm;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.common.sharding.starter.utils.SequenceShardKeyResolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;


public class OvelinDsStandardShardingAlgorithmTest extends AbstractStandardShardingAlgorithmTest{
    private final static OvelinDsStandardShardingAlgorithm ALGORITHM_2_2 = new OvelinDsStandardShardingAlgorithm();

    private final static OvelinDsStandardShardingAlgorithm ALGORITHM_4_2 = new OvelinDsStandardShardingAlgorithm();
    private final static OvelinDsStandardShardingAlgorithm ALGORITHM_4_4 = new OvelinDsStandardShardingAlgorithm();

   @BeforeAll
    static void setUp() {
        ALGORITHM_2_2.init(CONFIG_2_2.properties());
        ALGORITHM_4_2.init(CONFIG_4_2.properties());
        ALGORITHM_4_4.init(CONFIG_4_4.properties());

    }
    @Test
    void shouldShardingShardedIdConfig_2_2(){
        shouldShardingShardedId(ALGORITHM_2_2, CONFIG_2_2, ShardedId.of(0, System.currentTimeMillis()), "ds0");
        shouldShardingShardedId(ALGORITHM_2_2, CONFIG_2_2, ShardedId.of(1, System.currentTimeMillis()), "ds0");
        shouldShardingShardedId(ALGORITHM_2_2, CONFIG_2_2, ShardedId.of(2, System.currentTimeMillis()), "ds1");
        shouldShardingShardedId(ALGORITHM_2_2, CONFIG_2_2, ShardedId.of(3, System.currentTimeMillis()), "ds1");
        shouldShardingShardedId(ALGORITHM_2_2, CONFIG_2_2, ShardedId.of(4, System.currentTimeMillis()), "ds0");
        shouldShardingShardedId(ALGORITHM_2_2, CONFIG_2_2, ShardedId.of(5, System.currentTimeMillis()), "ds0");
        shouldShardingShardedId(ALGORITHM_2_2, CONFIG_2_2, ShardedId.of(6, System.currentTimeMillis()), "ds1");
        shouldShardingShardedId(ALGORITHM_2_2, CONFIG_2_2, ShardedId.of(7, System.currentTimeMillis()), "ds1");
    }
    @Test
    void shouldShardingShardedIdConfig_4_2(){
        shouldShardingShardedId(ALGORITHM_4_2, CONFIG_4_2, ShardedId.of(0, System.currentTimeMillis()), "ds0");
        shouldShardingShardedId(ALGORITHM_4_2, CONFIG_4_2, ShardedId.of(1, System.currentTimeMillis()), "ds0");
        shouldShardingShardedId(ALGORITHM_4_2, CONFIG_4_2, ShardedId.of(2, System.currentTimeMillis()), "ds1");
        shouldShardingShardedId(ALGORITHM_4_2, CONFIG_4_2, ShardedId.of(3, System.currentTimeMillis()), "ds1");
        shouldShardingShardedId(ALGORITHM_4_2, CONFIG_4_2, ShardedId.of(4, System.currentTimeMillis()), "ds2");
        shouldShardingShardedId(ALGORITHM_4_2, CONFIG_4_2, ShardedId.of(5, System.currentTimeMillis()), "ds2");
        shouldShardingShardedId(ALGORITHM_4_2, CONFIG_4_2, ShardedId.of(6, System.currentTimeMillis()), "ds3");
        shouldShardingShardedId(ALGORITHM_4_2, CONFIG_4_2, ShardedId.of(7, System.currentTimeMillis()), "ds3");
    }
    @Test
    void shouldShardingShardedIdConfig_4_4(){
        shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(0, System.currentTimeMillis()), "ds0");
        shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(1, System.currentTimeMillis()), "ds0");
        shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(2, System.currentTimeMillis()), "ds0");
        shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(3, System.currentTimeMillis()), "ds0");
        shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(4, System.currentTimeMillis()), "ds1");
        shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(5, System.currentTimeMillis()), "ds1");
        shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(6, System.currentTimeMillis()), "ds1");
        shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(7, System.currentTimeMillis()), "ds1");
        shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(8, System.currentTimeMillis()), "ds2");
        shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(9, System.currentTimeMillis()), "ds2");
        shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(10, System.currentTimeMillis()), "ds2");
        shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(11, System.currentTimeMillis()), "ds2");
   }


    @Test
    void shouldShardingStringKeyConfig_2_2(){
        List<SequenceShardKeyResolver.ShardKeyRecord> shardKeys = SequenceShardKeyResolver.getSequenceShardKeys(4);
        shouldShardingShardedId(shardKeys.get(0), ALGORITHM_2_2, CONFIG_2_2, "ds0");
        shouldShardingShardedId(shardKeys.get(1), ALGORITHM_2_2, CONFIG_2_2, "ds0");
        shouldShardingShardedId(shardKeys.get(2), ALGORITHM_2_2, CONFIG_2_2, "ds1");
        shouldShardingShardedId(shardKeys.get(3), ALGORITHM_2_2, CONFIG_2_2, "ds1");
    }

    @Test
    void shouldShardingStringFieldConfig_4_2(){
        List<SequenceShardKeyResolver.ShardKeyRecord> shardKeys = SequenceShardKeyResolver.getSequenceShardKeys(8);
        shouldShardingShardedId(shardKeys.get(0), ALGORITHM_4_2, CONFIG_4_2, "ds0");
        shouldShardingShardedId(shardKeys.get(1), ALGORITHM_4_2, CONFIG_4_2, "ds0");
        shouldShardingShardedId(shardKeys.get(2), ALGORITHM_4_2, CONFIG_4_2, "ds1");
        shouldShardingShardedId(shardKeys.get(3), ALGORITHM_4_2, CONFIG_4_2, "ds1");
        shouldShardingShardedId(shardKeys.get(4), ALGORITHM_4_2, CONFIG_4_2, "ds2");
        shouldShardingShardedId(shardKeys.get(5), ALGORITHM_4_2, CONFIG_4_2, "ds2");
        shouldShardingShardedId(shardKeys.get(6), ALGORITHM_4_2, CONFIG_4_2, "ds3");
        shouldShardingShardedId(shardKeys.get(7), ALGORITHM_4_2, CONFIG_4_2, "ds3");

    }
    @Test
    void shouldShardingStringFieldConfig_4_4(){
        List<SequenceShardKeyResolver.ShardKeyRecord> shardKeys = SequenceShardKeyResolver.getSequenceShardKeys(16);
        shouldShardingShardedId(shardKeys.get(0), ALGORITHM_4_4, CONFIG_4_4, "ds0");
        shouldShardingShardedId(shardKeys.get(1), ALGORITHM_4_4, CONFIG_4_4, "ds0");
        shouldShardingShardedId(shardKeys.get(2), ALGORITHM_4_4, CONFIG_4_4, "ds0");
        shouldShardingShardedId(shardKeys.get(3), ALGORITHM_4_4, CONFIG_4_4, "ds0");
        shouldShardingShardedId(shardKeys.get(4), ALGORITHM_4_4, CONFIG_4_4, "ds1");
        shouldShardingShardedId(shardKeys.get(5), ALGORITHM_4_4, CONFIG_4_4, "ds1");
        shouldShardingShardedId(shardKeys.get(6), ALGORITHM_4_4, CONFIG_4_4, "ds1");
        shouldShardingShardedId(shardKeys.get(7), ALGORITHM_4_4, CONFIG_4_4, "ds1");


    }
    void shouldShardingShardedId(SequenceShardKeyResolver.ShardKeyRecord shardKeyRecord, OvelinDsStandardShardingAlgorithm ovelinDsComplexShardingAlgorithm, Config config, String tables) {
        super.shouldShardingShardedId(shardKeyRecord, ovelinDsComplexShardingAlgorithm, config.dsNames(), tables);
    }
    void shouldShardingShardedId(OvelinDsStandardShardingAlgorithm ovelinDsComplexShardingAlgorithm, Config config, String shardingKey, String ds) {
        super.shouldShardingShardedId(ovelinDsComplexShardingAlgorithm, config.dsNames(), shardingKey, ds);
    }

    void shouldShardingShardedId(OvelinDsStandardShardingAlgorithm ovelinDsComplexShardingAlgorithm, Config config, ShardedId shardedId, String ds) {
       super.shouldShardingShardedId(ovelinDsComplexShardingAlgorithm, config.dsNames(), shardedId, ds);
    }
}
