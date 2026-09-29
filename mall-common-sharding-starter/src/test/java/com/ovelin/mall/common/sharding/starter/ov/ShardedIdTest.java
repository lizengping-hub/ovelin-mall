package com.ovelin.mall.common.sharding.starter.ov;

import com.ovelin.mall.common.sharding.core.ov.ShardId;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import org.junit.jupiter.api.Test;

public class ShardedIdTest {

    @Test
    public void testIdLayout() {
        long sequenceValue = 12345L;
        ShardId shardId = ShardId.of(100);
        ShardedId shardedId = ShardedId.of(shardId, sequenceValue);
        assert shardedId.shardId().value() >= 0;
        assert shardedId.sequence() == sequenceValue;
        System.out.println("Generated ID: " + shardedId);
    }
}
