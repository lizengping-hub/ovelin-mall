package com.ovelin.mall.common.sharding.core.core;

import com.ovelin.mall.common.sharding.core.api.ShardResolver;
import com.ovelin.mall.common.sharding.core.ov.ShardId;

import java.util.concurrent.ThreadLocalRandom;

public class RandomShardResolver implements ShardResolver {
    public ShardId currentShard() {
        return new ShardId(ThreadLocalRandom.current().nextInt(ShardId.SHARD_CAPACITY));
    }
}