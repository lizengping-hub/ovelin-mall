package com.ovelin.mall.id.generator.starter.domain.service;

import com.ovelin.mall.common.sharding.core.api.ShardSelector;
import com.ovelin.mall.common.sharding.core.ov.ShardId;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.Allocations;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.SequenceName;

import java.util.List;

public class IdGeneratorDomainService {
    private final SequenceDomainService sequenceService;
    private final ShardSelector shardSelector;
    public IdGeneratorDomainService(SequenceDomainService sequenceService, ShardSelector shardSelector) {
        this.shardSelector = shardSelector;
        this.sequenceService = sequenceService;
    }

    public List<ShardedId> nextIds(SequenceName name, ShardId shard, int count) {
        if (count <= 0) throw new IllegalArgumentException("count must be > 0");
        ShardId target = shard != null ? shard : shardSelector.select();

        Allocations allocations = sequenceService.nextValues(name, target, count);

        return allocations.toList().stream()
                .map(v -> ShardedId.of(target, v))
                .toList();
    }


    public ShardedId nextId(SequenceName name, ShardId shard) {
        ShardId target = shard != null ? shard : shardSelector.select();
        long sequenceValue = sequenceService.nextValue(name, target);
        return ShardedId.of(target, sequenceValue);
    }
}
