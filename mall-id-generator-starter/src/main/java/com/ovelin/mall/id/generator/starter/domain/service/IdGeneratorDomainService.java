package com.ovelin.mall.id.generator.starter.domain.service;

import com.ovelin.mall.id.generator.starter.domain.module.valueobject.Allocations;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.SequenceName;
import com.ovelin.mall.sharding.starter.api.ShardedId;
import java.util.List;

public class IdGeneratorDomainService {
    private final SequenceDomainService sequenceService;

    public IdGeneratorDomainService(SequenceDomainService sequenceService) {
        this.sequenceService = sequenceService;
    }


    public List<ShardedId> nextIds(String bizType, int count) {
        if (count <= 0) throw new IllegalArgumentException("count must be > 0");
        SequenceName sequenceName = new SequenceName(bizType);
        Allocations allocations = sequenceService.nextValues(sequenceName, count);
        return allocations.toList().stream().map(ShardedId::fromSequence).toList();
    }


    public ShardedId nextId(String bizType) {
        SequenceName sequenceName = new SequenceName(bizType);
        long sequenceValue = sequenceService.nextValue(sequenceName);
        return ShardedId.fromSequence(sequenceValue);
    }
}
