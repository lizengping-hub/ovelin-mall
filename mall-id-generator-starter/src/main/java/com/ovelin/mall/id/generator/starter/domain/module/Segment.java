package com.ovelin.mall.id.generator.starter.domain.module;

import com.ovelin.mall.id.generator.starter.domain.module.valueobject.Allocation;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a segment of a sequence with a minimum and maximum value.
 * [minValue, maxValue] is inclusive.
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

    /**
     * [minValue, maxValue] is inclusive.
     */
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


    public long remaining() {
        return Math.max(0, maxValue - nextValue.get() + 1);
    }
    public boolean hasNext() {
        return nextValue.get() <= maxValue;
    }
    public Allocation next() {
        while (true) {
            long current = nextValue.get();
            if (current > maxValue) {
                return new Allocation(current, 0); // exhausted
            }
            if (nextValue.compareAndSet(current, current + 1)) {
                return new Allocation(current, 1);
            }
        }
    }
    /**
     * Atomically allocate n consecutive IDs from this segment and return the first value.
     * Caller must ensure n > 0 and handle the case when remaining() < n.
     */
    public Allocation nextN(int n) {
        if (n <= 0) throw new IllegalArgumentException("n must be > 0");
        while (true) {
            long current = nextValue.get();
            if (current > maxValue) {
                return new Allocation(current, 0); // exhausted
            }
            long available = maxValue - current + 1;
            long take = Math.min(n, available);      // 能给多少就给多少
            if (nextValue.compareAndSet(current, current + take)) {
                return new Allocation(current, (int) take); // caller receives [current .. current + n -1]
            }
        }
    }



    @Override
    public String toString() {
        return "Segment[minValue=%d, maxValue=%d]".formatted(minValue, maxValue);
    }
}
