package com.ovelin.mall.id.generator.starter.infrastructure.persistence.repository;

import com.ovelin.mall.common.sharding.core.ov.ShardId;
import com.ovelin.mall.id.generator.starter.autoconfigure.IdGeneratorProperties;
import com.ovelin.mall.id.generator.starter.domain.module.Segment;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.SequenceName;
import com.ovelin.mall.id.generator.starter.domain.repository.SequenceRepository;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class MemorySequenceRepository implements SequenceRepository {
    private IdGeneratorProperties properties;
    public MemorySequenceRepository(IdGeneratorProperties properties) {
        this.properties = properties;
    }
    final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();

    @Override
    public Segment allocateSegment(SequenceName name, ShardId shardId) {
        AtomicLong counter = counters.computeIfAbsent(name.toString()+shardId, k -> new AtomicLong(0));
        while (true) {
            long current = counter.get();
            if (counter.compareAndSet(current, current + properties.allocationSize())) {
                return Segment.fromStartAndSize(current, properties.allocationSize());
            }
        }
    }
}