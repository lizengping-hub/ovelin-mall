package com.ovelin.mall.common.sharding.core.core;

import com.ovelin.mall.common.sharding.core.api.ShardSelector;
import com.ovelin.mall.common.sharding.core.ov.ShardId;

import java.util.concurrent.ThreadLocalRandom;

public class RandomShardSelector implements ShardSelector {
    public ShardId select() {
        return new ShardId(ThreadLocalRandom.current().nextInt(ShardId.SHARD_CAPACITY));
    }
}