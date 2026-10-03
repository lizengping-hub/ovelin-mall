package com.ovelin.mall.id.generator.starter.infrastructure.autoconfigure;


import com.ovelin.mall.common.sharding.core.api.ShardSelector;
import com.ovelin.mall.id.generator.api.IdGenerator;
import com.ovelin.mall.id.generator.starter.IdGeneratorAppService;
import com.ovelin.mall.id.generator.starter.domain.repository.SequenceRepository;
import com.ovelin.mall.id.generator.starter.domain.service.IdGeneratorDomainService;
import com.ovelin.mall.id.generator.starter.domain.service.SequenceDomainService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(IdGeneratorProperties.class)
public class IdGeneratorAutoconfiguration {
    @Bean
    IdGenerator idGenerator(IdGeneratorDomainService idGeneratorDomainService) {
        return new IdGeneratorAppService(idGeneratorDomainService);
    }
    @Bean
    IdGeneratorDomainService idGeneratorDomainService(SequenceDomainService sequenceService, ShardSelector shardSelector) {
        return new IdGeneratorDomainService(sequenceService, shardSelector);
    }
    @Bean
    SequenceDomainService sequenceDomainService(SequenceRepository sequenceRepository) {
        return new SequenceDomainService(sequenceRepository);
    }
}
