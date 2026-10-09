package com.ovelin.mall.id.generator.starter;

import com.ovelin.mall.common.sharding.core.ov.ShardId;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.id.generator.api.IdGenerationCommand;
import com.ovelin.mall.id.generator.api.IdGenerator;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.SequenceName;
import com.ovelin.mall.id.generator.starter.domain.service.IdGeneratorDomainService;

import java.util.List;

public class IdGeneratorAppService implements IdGenerator {

    private final IdGeneratorDomainService idGeneratorDomainService;

    public IdGeneratorAppService(IdGeneratorDomainService idGeneratorDomainService) {
        this.idGeneratorDomainService = idGeneratorDomainService;
    }

    @Override
    public List<ShardedId> nextIds(IdGenerationCommand command) {
        ShardId shardId = command.shardId().orElse(null);
        SequenceName sequenceName = IdAssembler.toSequenceName(command.businessType());
        List<ShardedId> sharedIds = idGeneratorDomainService.nextIds(sequenceName, shardId, command.count());
        return sharedIds.stream().toList();
    }

    @Override
    public ShardedId nextId(IdGenerationCommand command) {
        SequenceName sequenceName = IdAssembler.toSequenceName(command.businessType());
        ShardId shardId = command.shardId().orElse(null);
        return idGeneratorDomainService.nextId(sequenceName, shardId);
    }
}
