package com.ovelin.mall.sharding.starter.api;

import org.junit.jupiter.api.Test;

public class ShardedIdTest {

    @Test
    public void testIdLayout() {
        long sequenceValue = 12345L;
        ShardedId shardedId = ShardedId.fromSequence(sequenceValue);
        long id = shardedId.getId();
        assert (shardedId.getShardId()) >= 0;
        assert (shardedId.getSequence()) == sequenceValue;
        System.out.println("Generated ID: " + shardedId);
    }
}
