package com.ovelin.mall.common.sharding.starter.algorithm;


import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.common.sharding.starter.utils.SequenceShardKeyResolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class OvelinDsComplexShardingAlgorithmTest extends AbstractComplexShardingAlgorithmTest{
    private final static OvelinDsComplexShardingAlgorithm ALGORITHM_2_2 = new OvelinDsComplexShardingAlgorithm();
    private final static OvelinDsComplexShardingAlgorithm ALGORITHM_4_2 = new OvelinDsComplexShardingAlgorithm();
    private final static OvelinDsComplexShardingAlgorithm ALGORITHM_4_4 = new OvelinDsComplexShardingAlgorithm();
    @BeforeAll
    static void setUp() {
        ALGORITHM_2_2.init(CONFIG_2_2.properties());


        ALGORITHM_4_2.init(CONFIG_4_2.properties());

        ALGORITHM_4_4.init(CONFIG_4_4.properties());


    }
    @Test
    void shouldShardingShardedIdConfig2_2(){
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
    void shouldShardingShardedIdConfig4_2(){
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
    void shouldShardingShardedIdConfig4_4(){
        shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(0, System.currentTimeMillis()), "ds0");
        shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(1, System.currentTimeMillis()), "ds0");
        shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(2, System.currentTimeMillis()), "ds0");
        shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(3, System.currentTimeMillis()), "ds0");
        shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(4, System.currentTimeMillis()), "ds1");
        shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(5, System.currentTimeMillis()), "ds1");
        shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(6, System.currentTimeMillis()), "ds1");
        shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(7, System.currentTimeMillis()), "ds1");
    }


    @Test
    void shouldShardingStringKeyConfig_2_2(){
        List<SequenceShardKeyResolver.ShardKeyRecord> shardKeys = SequenceShardKeyResolver.getSequenceShardKeys(4);
        shouldShardingStringKey(shardKeys.get(0), ALGORITHM_2_2, CONFIG_2_2, "ds0");
        shouldShardingStringKey(shardKeys.get(1), ALGORITHM_2_2, CONFIG_2_2, "ds0");
        shouldShardingStringKey(shardKeys.get(2), ALGORITHM_2_2, CONFIG_2_2, "ds1");
        shouldShardingStringKey(shardKeys.get(3), ALGORITHM_2_2, CONFIG_2_2, "ds1");
    }
    @Test
    void shouldShardingStringKeyConfig_4_2(){
        List<SequenceShardKeyResolver.ShardKeyRecord> shardKeys = SequenceShardKeyResolver.getSequenceShardKeys(8);
        shouldShardingStringKey(shardKeys.get(0), ALGORITHM_4_2, CONFIG_4_2, "ds0");
        shouldShardingStringKey(shardKeys.get(1), ALGORITHM_4_2, CONFIG_4_2, "ds0");
        shouldShardingStringKey(shardKeys.get(2), ALGORITHM_4_2, CONFIG_4_2, "ds1");
        shouldShardingStringKey(shardKeys.get(3), ALGORITHM_4_2, CONFIG_4_2, "ds1");
        shouldShardingStringKey(shardKeys.get(4), ALGORITHM_4_2, CONFIG_4_2, "ds2");
        shouldShardingStringKey(shardKeys.get(5), ALGORITHM_4_2, CONFIG_4_2, "ds2");
        shouldShardingStringKey(shardKeys.get(6), ALGORITHM_4_2, CONFIG_4_2, "ds3");
        shouldShardingStringKey(shardKeys.get(7), ALGORITHM_4_2, CONFIG_4_2, "ds3");
    }

    @Test
    void shouldShardingStringFieldConfig_4_4(){
        List<SequenceShardKeyResolver.ShardKeyRecord> shardKeys = SequenceShardKeyResolver.getSequenceShardKeys(16);
        shouldShardingStringKey(shardKeys.get(0), ALGORITHM_4_4, CONFIG_4_4, "ds0");
        shouldShardingStringKey(shardKeys.get(1), ALGORITHM_4_4, CONFIG_4_4, "ds0");
        shouldShardingStringKey(shardKeys.get(2), ALGORITHM_4_4, CONFIG_4_4, "ds0");
        shouldShardingStringKey(shardKeys.get(3), ALGORITHM_4_4, CONFIG_4_4, "ds0");
        shouldShardingStringKey(shardKeys.get(4), ALGORITHM_4_4, CONFIG_4_4, "ds1");
        shouldShardingStringKey(shardKeys.get(5), ALGORITHM_4_4, CONFIG_4_4, "ds1");
        shouldShardingStringKey(shardKeys.get(6), ALGORITHM_4_4, CONFIG_4_4, "ds1");
        shouldShardingStringKey(shardKeys.get(7), ALGORITHM_4_4, CONFIG_4_4, "ds1");
    }

    void shouldShardingStringKey(SequenceShardKeyResolver.ShardKeyRecord shardKey, OvelinDsComplexShardingAlgorithm ovelinDsComplexShardingAlgorithm, Config config, String ds) {
        super.shouldShardingStringKey(shardKey, ovelinDsComplexShardingAlgorithm, config.dsNames(), ds);
    }

    void shouldShardingShardedId(OvelinDsComplexShardingAlgorithm ovelinDsComplexShardingAlgorithm, Config config, ShardedId shardedId, String shardingKey, String ds) {
        super.shouldShardingShardedId(ovelinDsComplexShardingAlgorithm, config.dsNames(), shardedId, shardingKey, ds);
    }

    void shouldShardingShardedId(OvelinDsComplexShardingAlgorithm ovelinDsComplexShardingAlgorithm, Config config, String shardingKey, String ds) {
        super.shouldShardingShardedId(ovelinDsComplexShardingAlgorithm, config.dsNames(), shardingKey, ds);
    }

    void shouldShardingShardedId(OvelinDsComplexShardingAlgorithm ovelinDsComplexShardingAlgorithm, Config config, ShardedId shardedId, String ds) {
        super.shouldShardingShardedId(ovelinDsComplexShardingAlgorithm, config.dsNames(), shardedId, ds);
    }
}
