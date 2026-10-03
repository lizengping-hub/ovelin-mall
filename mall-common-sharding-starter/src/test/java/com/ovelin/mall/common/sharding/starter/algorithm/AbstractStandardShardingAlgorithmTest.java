package com.ovelin.mall.common.sharding.starter.algorithm;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.common.sharding.starter.utils.SequenceShardKeyResolver;
import org.apache.shardingsphere.infra.datanode.DataNodeInfo;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AbstractStandardShardingAlgorithmTest extends AbstractShardingAlgorithmTest{
    void shouldShardingShardedId(SequenceShardKeyResolver.ShardKeyRecord shardKeyRecord, AbstractOvelinStandardShardingAlgorithm ovelinDsComplexShardingAlgorithm, Set<String> availableTargetNames
            , String tables) {
        shouldShardingShardedId(ovelinDsComplexShardingAlgorithm, availableTargetNames, shardKeyRecord.shardKey(), tables);
        shouldShardingShardedId(ovelinDsComplexShardingAlgorithm, availableTargetNames, ShardedId.of(shardKeyRecord.shardId(), System.currentTimeMillis()), tables);
    }
    void shouldShardingShardedId(AbstractOvelinStandardShardingAlgorithm ovelinDsComplexShardingAlgorithm, Set<String> availableTargetNames, String shardingKey, String ds) {

        DataNodeInfo dataNodeInfo = new DataNodeInfo("table", 1, 'g');
        PreciseShardingValue<Comparable<?>> preciseShardingValue = new PreciseShardingValue<>("table", "k2", dataNodeInfo, shardingKey);

        String resultDsName = ovelinDsComplexShardingAlgorithm.doSharding(availableTargetNames, preciseShardingValue);
        assertEquals(ds, resultDsName);
    }

    void shouldShardingShardedId(AbstractOvelinStandardShardingAlgorithm ovelinDsComplexShardingAlgorithm, Set<String> availableTargetNames, ShardedId shardedId, String ds) {
        DataNodeInfo dataNodeInfo = new DataNodeInfo("table", 1, 'g');
        PreciseShardingValue<Comparable<?>> preciseShardingValue = new PreciseShardingValue<>("table", "k2", dataNodeInfo,
                shardedId.value());

        String resultDsName = ovelinDsComplexShardingAlgorithm.doSharding(availableTargetNames, preciseShardingValue);
        assertEquals(ds, resultDsName);
    }
}
