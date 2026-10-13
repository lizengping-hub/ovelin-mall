package com.ovelin.mall.common.sharding.starter.algorithm;


import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.common.sharding.starter.utils.SequenceShardKeyResolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ovelin.mall.common.sharding.starter.algorithm.ShardingAlgorithmComplexTwoKeyTestConfig.*;

public class OvelinComplexDsTwoKeyShardingAlgorithmTest extends AbstractComplexShardingAlgorithmTest{
    private final static OvelinComplexDsShardingAlgorithm ALGORITHM_2_2 = new OvelinComplexDsShardingAlgorithm();
    private final static OvelinComplexDsShardingAlgorithm ALGORITHM_4_2 = new OvelinComplexDsShardingAlgorithm();
    private final static OvelinComplexDsShardingAlgorithm ALGORITHM_4_4 = new OvelinComplexDsShardingAlgorithm();
    @BeforeAll
    static void setUp() {
        ALGORITHM_2_2.init(CONFIG_2_2.properties());
        ALGORITHM_4_2.init(CONFIG_4_2.properties());
        ALGORITHM_4_4.init(CONFIG_4_4.properties());
    }
    @Test
    void shouldShardingShardedIdConfig2_2(){
        String[] dss = {"ds0", "ds0", "ds1", "ds1", "ds0", "ds0", "ds1", "ds1"};
        shouldShardingShardedId(dss, ALGORITHM_2_2, CONFIG_2_2);
    }

    @Test
    void shouldShardingShardedIdConfig4_2(){
        String[] dss = {"ds0", "ds0", "ds1", "ds1", "ds2", "ds2", "ds3", "ds3"};
        shouldShardingShardedId(dss, ALGORITHM_4_2, CONFIG_4_2);
    }


    @Test
    void shouldShardingShardedIdConfig4_4(){
        String[] dss = {"ds0", "ds0", "ds0", "ds0", "ds1", "ds1", "ds1", "ds1"};
        shouldShardingShardedId(dss, ALGORITHM_4_4, CONFIG_4_4);
    }

    private void shouldShardingShardedId(String[] dss, OvelinComplexDsShardingAlgorithm algorithm, Config config){
        for (int i = 0; i < dss.length; i++) {
            shouldShardingShardedId(algorithm, config, ShardedId.of(i, System.currentTimeMillis()), dss[i]);
        }
    }


    @Test
    void shouldShardingStringKeyConfig_2_2(){
        List<SequenceShardKeyResolver.ShardKey2Record> shardKeys = SequenceShardKeyResolver.getSequenceTwoShardKeys(4);
        String[] dss = {"ds0", "ds0", "ds1", "ds1"};
        shouldShardingStringKey(shardKeys, dss, ALGORITHM_2_2, CONFIG_2_2);
    }
    @Test
    void shouldShardingStringKeyConfig_4_2(){
        List<SequenceShardKeyResolver.ShardKey2Record> shardKeys = SequenceShardKeyResolver.getSequenceTwoShardKeys(8);
        String[] dss = {"ds0", "ds0", "ds1", "ds1", "ds2", "ds2", "ds3", "ds3"};
        shouldShardingStringKey(shardKeys, dss, ALGORITHM_4_2, CONFIG_4_2);
    }

    @Test
    void shouldShardingStringFieldConfig_4_4(){
        List<SequenceShardKeyResolver.ShardKey2Record> shardKeys = SequenceShardKeyResolver.getSequenceTwoShardKeys(16);
        String[] dss = {"ds0", "ds0", "ds0", "ds0", "ds1", "ds1", "ds1", "ds1"};
        shouldShardingStringKey(shardKeys, dss, ALGORITHM_4_4, CONFIG_4_4);
    }
    private void shouldShardingStringKey(List<SequenceShardKeyResolver.ShardKey2Record> shardKeys, String[] dss, OvelinComplexDsShardingAlgorithm algorithm, Config config){
        for (int i = 0; i < dss.length; i++) {
            shouldShardingStringKey(shardKeys.get(i), algorithm, config, dss[i]);
        }
    }
    void shouldShardingStringKey(SequenceShardKeyResolver.ShardKey2Record shardKey, OvelinComplexDsShardingAlgorithm ovelinDsComplexShardingAlgorithm, Config config, String ds) {
        super.shouldShardingStringKey(shardKey, ovelinDsComplexShardingAlgorithm, config.dsNames(), ds);
    }

    void shouldShardingShardedId(OvelinComplexDsShardingAlgorithm ovelinDsComplexShardingAlgorithm, Config config, ShardedId shardedId, String ds) {
        super.shouldShardingShardedId(ovelinDsComplexShardingAlgorithm, config.dsNames(), ds, shardedId, null, null);
    }
}
