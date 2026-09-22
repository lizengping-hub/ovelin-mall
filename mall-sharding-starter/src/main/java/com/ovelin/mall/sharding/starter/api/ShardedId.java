package com.ovelin.mall.sharding.starter.api;

import com.ovelin.mall.ddd.kernel.Identifier;

import java.util.concurrent.ThreadLocalRandom;

public final class ShardedId implements Identifier {

    public static final int SHARD_BITS = 15;
    public static final int SEQUENCE_BITS = 48;

    /**
     * SHARD_CAPACITY 表示分片的容量，即可以支持的最大分片数量。它是通过将 1 左移 SHARD_BITS 位来计算的。
     */
    public static final int SHARD_CAPACITY = 1 << SHARD_BITS;      // 32768，[0, bound) 左闭右开
    public static final int MAX_SHARD_ID = SHARD_CAPACITY - 1;      // 32767，[0, max] 闭区间上界

    public static final long SHARD_MASK = MAX_SHARD_ID;

    public static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;

    private final long id;
    private ShardedId(long id) {
        this.id = id;
    }
    public static ShardedId fromId(long id) {
        return new ShardedId(id);
    }
    public static ShardedId from(long shardId, long sequenceValue) {
        long id = (sequenceValue << SHARD_BITS) | shardId;
        return new ShardedId(id);
    }
    public static ShardedId fromSequence(long sequenceValue) {
        long shardId = shardId();
        validateSequence(sequenceValue);
        long id = (sequenceValue << SHARD_BITS) | shardId;
        return new ShardedId(id);
    }
    private static long shardId() {
        return ThreadLocalRandom.current().nextLong(SHARD_CAPACITY);
    }

    private static void validateSequence(long sequenceValue) {
        if (sequenceValue < 0) {
            throw new IllegalArgumentException(
                    "Sequence value must be non-negative."
            );
        }
        if (sequenceValue > MAX_SEQUENCE) {
            throw new IllegalArgumentException(
                    "Sequence value exceeds the maximum allowed value."
            );
        }
    }
    public long getId() {
        return id;
    }
    public long getShardId() {
        return id & SHARD_MASK;
    }
    public long getSequence() {
        return id >> SHARD_BITS;
    }
    @Override
    public String toString() {
        return "0x"+Long.toHexString(id);
    }
}