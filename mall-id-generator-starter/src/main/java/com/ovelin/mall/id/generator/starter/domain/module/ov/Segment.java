package com.ovelin.mall.id.generator.starter.domain.module.ov;

import com.ovelin.mall.sharding.starter.api.ShardedId;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a segment of a sequence with a minimum and maximum value.
 */
public final class Segment {
    private final long minValue;
    private final long maxValue;
    private final AtomicLong nextValue;

    private Segment(long minValue, long maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.nextValue = new AtomicLong(minValue);
    }

    public static Segment of(long minValue, long maxValue) {
        validate(minValue, maxValue);
        return new Segment(minValue, maxValue);
    }

    public static Segment fromStartAndSize(long minValue, long size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }
        return of(minValue, minValue + size - 1);
    }

    private static void validate(long minValue, long maxValue) {
        if (minValue < 0 || maxValue < 0) {
            throw new IllegalArgumentException("minValue and maxValue must be non-negative");
        }
        if (minValue > maxValue) {
            throw new IllegalArgumentException("minValue cannot be greater than maxValue");
        }
    }

    public long size() {
        return maxValue - minValue + 1;
    }
    public long minValue() {
        return minValue;
    }
    public long maxValue() {
        return maxValue;
    }
    public boolean hasNext() {
        return nextValue.get() <= maxValue;
    }

    public long next() {
        while (true) {
            long current = nextValue.get();
            if (current > maxValue) {
                throw new IllegalStateException("Segment is exhausted: " + this);
            }
            if (nextValue.compareAndSet(current, current + 1)) {
                return current;
            }
        }
    }

    public long remaining() {
        return Math.max(0, maxValue - nextValue.get() + 1);
    }

    @Override
    public String toString() {
        return "Segment[minValue=%d, maxValue=%d]".formatted(minValue, maxValue);
    }
}
