package com.ovelin.mall.common.sharding.starter.algorithm;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.common.sharding.starter.utils.SequenceShardKeyResolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.ovelin.mall.common.sharding.starter.algorithm.ShardingAlgorithmTestConfig.*;


public class OvelinStandardDsShardingAlgorithmTest extends AbstractStandardShardingAlgorithmTest {
    private final static OvelinStandardDsShardingAlgorithm ALGORITHM_2_2 = new OvelinStandardDsShardingAlgorithm();

    private final static OvelinStandardDsShardingAlgorithm ALGORITHM_4_2 = new OvelinStandardDsShardingAlgorithm();
    private final static OvelinStandardDsShardingAlgorithm ALGORITHM_4_4 = new OvelinStandardDsShardingAlgorithm();

    @BeforeAll
    static void setUp() {
        ALGORITHM_2_2.init(CONFIG_2_2.properties());
        ALGORITHM_4_2.init(CONFIG_4_2.properties());
        ALGORITHM_4_4.init(CONFIG_4_4.properties());

    }

    @Test
    void shouldShardingShardedIdConfig_2_2() {
        String[] dss = {"ds0", "ds0", "ds1", "ds1", "ds0", "ds0", "ds1", "ds1"};
        for (int i = 0; i < 8; i++) {
            shouldShardingShardedId(ALGORITHM_2_2, CONFIG_2_2, ShardedId.of(i, System.currentTimeMillis()), dss[i]);
        }
    }

    @Test
    void shouldShardingShardedIdConfig_4_2() {
        String[] dss = {"ds0", "ds0", "ds1", "ds1", "ds2", "ds2", "ds3", "ds3"};
        for (int i = 0; i < 8; i++) {
            shouldShardingShardedId(ALGORITHM_4_2, CONFIG_4_2, ShardedId.of(i, System.currentTimeMillis()), dss[i]);
        }
    }

    @Test
    void shouldShardingShardedIdConfig_4_4() {
        String[] dss = {"ds0", "ds0", "ds0", "ds0", "ds1", "ds1", "ds1", "ds1", "ds2", "ds2", "ds2", "ds2"};
        for (int i = 0; i < 12; i++) {
            shouldShardingShardedId(ALGORITHM_4_4, CONFIG_4_4, ShardedId.of(i, System.currentTimeMillis()), dss[i]);
        }
    }


    @Test
    void shouldShardingStringKeyConfig_2_2() {
        List<SequenceShardKeyResolver.ShardKeyRecord> shardKeys = SequenceShardKeyResolver.getSequenceShardKeys(4);
        String[] dss = {"ds0", "ds0", "ds1", "ds1"};
        for (int i = 0; i < 4; i++) {
            shouldShardingShardedId(shardKeys.get(i), ALGORITHM_2_2, CONFIG_2_2, dss[i]);
        }

    }

    @Test
    void shouldShardingStringFieldConfig_4_2() {
        List<SequenceShardKeyResolver.ShardKeyRecord> shardKeys = SequenceShardKeyResolver.getSequenceShardKeys(8);
        String[] dss = {"ds0", "ds0", "ds1", "ds1", "ds2", "ds2", "ds3", "ds3"};
        for (int i = 0; i < 8; i++) {
            shouldShardingShardedId(shardKeys.get(i), ALGORITHM_4_2, CONFIG_4_2, dss[i]);
        }

    }

    @Test
    void shouldShardingStringFieldConfig_4_4() {
        List<SequenceShardKeyResolver.ShardKeyRecord> shardKeys = SequenceShardKeyResolver.getSequenceShardKeys(16);
        String[] dss = {"ds0", "ds0", "ds0", "ds0", "ds1", "ds1", "ds1", "ds1"};
        for (int i = 0; i < 8; i++) {
            shouldShardingShardedId(shardKeys.get(i), ALGORITHM_4_4, CONFIG_4_4, dss[i]);
        }
    }

    void shouldShardingShardedId(SequenceShardKeyResolver.ShardKeyRecord shardKeyRecord, OvelinStandardDsShardingAlgorithm ovelinDsComplexShardingAlgorithm, Config config, String ds) {
        super.shouldShardingShardedId(shardKeyRecord, ovelinDsComplexShardingAlgorithm, config.dsNames(), ds);
    }

    void shouldShardingShardedId(OvelinStandardDsShardingAlgorithm ovelinDsComplexShardingAlgorithm, Config config, ShardedId shardedId, String ds) {
        super.shouldShardingShardedId(ovelinDsComplexShardingAlgorithm, config.dsNames(), shardedId.value(), ds);
    }
}
