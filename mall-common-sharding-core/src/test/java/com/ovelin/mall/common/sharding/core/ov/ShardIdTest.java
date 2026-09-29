package com.ovelin.mall.common.sharding.core.ov;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ShardIdTest {
    @Test
    void shouldRejectShardOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> ShardId.of(-1));
        assertThrows(IllegalArgumentException.class, () -> ShardId.of(IdLayout.MAX_SHARD + 1));
    }
    @Test
    void shouldAcceptValidShardId() {
        ShardId shardId = ShardId.of(0);
        assertEquals(0, shardId.value());
        shardId = ShardId.of(IdLayout.MAX_SHARD);
        assertEquals(IdLayout.MAX_SHARD, shardId.value());
    }
}
