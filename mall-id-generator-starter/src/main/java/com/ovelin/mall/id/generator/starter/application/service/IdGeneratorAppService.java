package com.ovelin.mall.id.generator.starter.application.service;

import com.ovelin.mall.id.generator.starter.application.assembler.IdAssembler;
import com.ovelin.mall.id.generator.starter.application.command.IdGenerationCommand;
import com.ovelin.mall.id.generator.starter.application.dto.IdDTO;
import com.ovelin.mall.id.generator.starter.application.port.out.IdGenerator;
import com.ovelin.mall.id.generator.starter.domain.service.IdGeneratorDomainService;
import com.ovelin.mall.id.generator.starter.domain.service.SequenceDomainService;
import com.ovelin.mall.sharding.starter.api.ShardedId;

import java.util.List;

public class IdGeneratorAppService implements IdGenerator {

    private final IdGeneratorDomainService idGeneratorDomainService;

    public IdGeneratorAppService(IdGeneratorDomainService idGeneratorDomainService) {
        this.idGeneratorDomainService = idGeneratorDomainService;
    }

    @Override
    public List<IdDTO> nextIds(IdGenerationCommand command) {
        List<ShardedId> sharedIds = idGeneratorDomainService.nextIds(command.businessType().value(), command.count());
        return sharedIds.stream().map(IdAssembler::toIdDTO).toList();
    }

    @Override
    public IdDTO nextId(IdGenerationCommand command) {
        ShardedId sharedId = idGeneratorDomainService.nextId(command.businessType().value());
        return IdAssembler.toIdDTO(sharedId);
    }
}
