package com.ovelin.mall.common.sharding.core.ov;

public record ShardId(int value) {
    public static final int SHARD_CAPACITY = IdLayout.SHARD_CAPACITY;
    public ShardId{
        if (value < 0 || value > IdLayout.MAX_SHARD) {
            throw new IllegalArgumentException("shardId must be between 0 and "+IdLayout.MAX_SHARD);
        }
    }
    public static  ShardId of(int value) {
        return new ShardId(value);
    }
}
