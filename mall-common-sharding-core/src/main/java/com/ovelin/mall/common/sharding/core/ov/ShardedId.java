package com.ovelin.mall.common.sharding.core.ov;

import com.ovelin.mall.ddd.kernel.Identifier;;

public final class ShardedId implements Identifier {
    private final long id;
    private ShardedId(long id) { this.id = id; }

    public static ShardedId fromId(long id) {
        requireNonNegative(id);
        return new ShardedId(id);
    }

    public static ShardedId of(ShardId shard, long sequence) {
        IdLayout.validateSequence(sequence);
        return new ShardedId((sequence << IdLayout.SHARD_BITS) | shard.value());
    }
    public static ShardedId of(int shard, long sequence) {
        return of(ShardId.of(shard), sequence);
    }
    public static int shardOf(long id) {
        requireNonNegative(id);
        return (int) (id & IdLayout.MAX_SHARD);
    }

    private static void requireNonNegative(long id) {
        if (id < 0) {
            throw new IllegalArgumentException("id must be non-negative: " + id);
        }
    }

    public ShardId shardId() { return new ShardId((int) (id & IdLayout.MAX_SHARD)); }
    public long sequence()   { return id >> IdLayout.SHARD_BITS; }
    public long value()      { return id; }
    @Override
    public int hashCode() { return Long.hashCode(id); }
    @Override
    public String toString() { return toHexString(); }
    public String toHexString() { return "0x"+Long.toHexString(id); }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ShardedId other = (ShardedId) obj;
        return this.id == other.id;
    }
}