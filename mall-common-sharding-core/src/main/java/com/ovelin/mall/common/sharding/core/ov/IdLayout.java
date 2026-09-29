package com.ovelin.mall.common.sharding.core.ov;

final class IdLayout {
    static final int SHARD_BITS = 15;
    static final int SEQUENCE_BITS = 48;
    static final int SHARD_CAPACITY = 1 << SHARD_BITS;
    static final int MAX_SHARD = SHARD_CAPACITY - 1;
    static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;
    private IdLayout() {}

    static void validateSequence(long sequenceValue) {
        if (sequenceValue < 0) {
            throw new IllegalArgumentException(
                    "Sequence value must be non-negative."
            );
        }
        if (sequenceValue > IdLayout.MAX_SEQUENCE) {
            throw new IllegalArgumentException(
                    "Sequence value exceeds the maximum allowed value."
            );
        }
    }
}
