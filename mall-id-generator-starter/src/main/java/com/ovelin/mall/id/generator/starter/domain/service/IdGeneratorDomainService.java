package com.ovelin.mall.id.generator.starter.domain.service;

import com.ovelin.mall.common.sharding.core.api.ShardResolver;
import com.ovelin.mall.common.sharding.core.ov.ShardId;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.Allocations;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.SequenceName;

import java.util.List;

public class IdGeneratorDomainService {
    private final SequenceDomainService sequenceService;
    private final ShardResolver shardResolver;
    public IdGeneratorDomainService(SequenceDomainService sequenceService, ShardResolver shardResolver) {
        this.shardResolver = shardResolver;
        this.sequenceService = sequenceService;
    }

    public List<ShardedId> nextIds(SequenceName name, ShardId shard, int count) {
        if (count <= 0) throw new IllegalArgumentException("count must be > 0");
        ShardId target = shard != null ? shard : shardResolver.currentShard();

        Allocations allocations = sequenceService.nextValues(name, target, count);

        return allocations.toList().stream()
                .map(v -> ShardedId.of(target, v))
                .toList();
    }


    public ShardedId nextId(SequenceName name, ShardId shard) {
        ShardId target = shard != null ? shard : shardResolver.currentShard();
        long sequenceValue = sequenceService.nextValue(name, target);
        return ShardedId.of(target, sequenceValue);
    }
}
