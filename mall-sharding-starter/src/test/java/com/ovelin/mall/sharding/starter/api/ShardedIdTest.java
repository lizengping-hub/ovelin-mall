package com.ovelin.mall.sharding.starter.api;

import org.junit.jupiter.api.Test;

public class ShardedIdTest {

    @Test
    public void testIdLayout() {
        long sequenceValue = 12345L;
        ShardedId idLayout = ShardedId.fromSequence(sequenceValue);
        long id = idLayout.getId();
        assert (idLayout.getShardId()) >= 0;
        assert (idLayout.getSequence()) == sequenceValue;
        System.out.println("Generated ID: " + idLayout);
    }
}
