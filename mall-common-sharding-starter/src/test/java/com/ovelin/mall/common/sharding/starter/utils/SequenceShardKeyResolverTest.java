package com.ovelin.mall.common.sharding.starter.utils;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SequenceShardKeyResolverTest {
    @Test
    void testGetEntriesForShardingKeys() {
        int shardCount = 8;
        List<SequenceShardKeyResolver.ShardKeyRecord> shardKeys = SequenceShardKeyResolver.getSequenceShardKeys(shardCount);
        assertThat(shardKeys.size()).isEqualTo(shardCount);
        for (int i = 0; i < shardKeys.size(); i++) {
            assertThat(shardKeys.get(i).shardIndex()).isEqualTo(i);
        }
    }

}
