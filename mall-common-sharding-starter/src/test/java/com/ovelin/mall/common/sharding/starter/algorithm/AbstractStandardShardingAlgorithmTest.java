package com.ovelin.mall.common.sharding.starter.algorithm;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.common.sharding.starter.utils.SequenceShardKeyResolver;
import org.apache.shardingsphere.infra.datanode.DataNodeInfo;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AbstractStandardShardingAlgorithmTest{
    void shouldShardingShardedId(SequenceShardKeyResolver.ShardKeyRecord shardKeyRecord, AbstractOvelinStandardShardingAlgorithm ovelinDsComplexShardingAlgorithm, Set<String> availableTargetNames
            , String targetName) {
        shouldShardingShardedId(ovelinDsComplexShardingAlgorithm, availableTargetNames, shardKeyRecord.shardKey(), targetName);
        shouldShardingShardedId(ovelinDsComplexShardingAlgorithm, availableTargetNames, ShardedId.of(shardKeyRecord.shardId(), System.currentTimeMillis()).value(), targetName);
    }

    void shouldShardingShardedId(AbstractOvelinStandardShardingAlgorithm ovelinDsComplexShardingAlgorithm, Set<String> availableTargetNames, Comparable<?> shardingKey, String targetName) {
        DataNodeInfo dataNodeInfo = new DataNodeInfo("table", 1, 'g');
        PreciseShardingValue<Comparable<?>> preciseShardingValue = new PreciseShardingValue<>("table", "k2", dataNodeInfo, shardingKey);

        String resultName = ovelinDsComplexShardingAlgorithm.doSharding(availableTargetNames, preciseShardingValue);
        assertEquals(targetName, resultName);
    }
}
