package com.ovelin.mall.sharding.starter.api;

import com.ovelin.mall.sharding.starter.api.excutor.ShardCalculator;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ShardCalculatorTest {

    private final ShardCalculator shardCalculator = new ShardCalculator();

    @Test
    void shardIdForIsDeterministicForSameKey() {
        int first = shardCalculator.shardIdFor("user-123");
        int second = shardCalculator.shardIdFor("user-123");
        assertThat(first).isEqualTo(second);
    }

    @Test
    void shardIdForIsWithinCapacityRange() {
        for (int i = 0; i < 1000; i++) {
            int shardId = shardCalculator.shardIdFor("key-" + i);
            assertThat(shardId).isBetween(0, ShardedId.MAX_SHARD_ID);
        }
    }

    @Test
    void shardIdForDistributesDifferentKeysAcrossShards() {
        // 抽样验证哈希分布不会把大量不同 key 全部塞进极少数 shard 里
        Set<Integer> shardIds = new HashSet<>();
        for (int i = 0; i < 2000; i++) {
            shardIds.add(shardCalculator.shardIdFor("username-" + i));
        }
        assertThat(shardIds.size()).isGreaterThan(500);
    }

    @Test
    void differentKeyTypesWithSameStringRepresentationHashTheSame() {
        // hashToShard 是基于 toString() 的，long 123 和 String "123" 应该落在同一个 shard
        int fromLong = shardCalculator.shardIdFor(123L);
        int fromString = shardCalculator.shardIdFor("123");
        assertThat(fromLong).isEqualTo(fromString);
    }
}
