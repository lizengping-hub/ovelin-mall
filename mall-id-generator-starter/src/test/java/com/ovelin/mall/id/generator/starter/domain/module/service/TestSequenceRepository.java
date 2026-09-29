package com.ovelin.mall.id.generator.starter.domain.module.service;

import com.ovelin.mall.common.sharding.core.ov.ShardId;
import com.ovelin.mall.id.generator.starter.domain.module.Segment;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.SequenceName;
import com.ovelin.mall.id.generator.starter.domain.repository.SequenceRepository;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class TestSequenceRepository implements SequenceRepository {
    long size = 100000;
    final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();

    @Override
    public Segment allocateSegment(SequenceName name, ShardId shardId) {
        AtomicLong counter = counters.computeIfAbsent(name.toString()+shardId, k -> new AtomicLong(0));
        while (true) {
            long current = counter.get();
            if (counter.compareAndSet(current, current + size)) {
                return Segment.fromStartAndSize(current, size);
            }
        }
    }
}