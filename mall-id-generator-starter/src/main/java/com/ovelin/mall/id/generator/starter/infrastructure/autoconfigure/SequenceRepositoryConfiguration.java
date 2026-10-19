package com.ovelin.mall.id.generator.starter.infrastructure.autoconfigure;

import com.ovelin.mall.id.generator.starter.domain.repository.SequenceRepository;
import com.ovelin.mall.id.generator.starter.infrastructure.persistence.mapper.IdSequenceMapper;
import com.ovelin.mall.id.generator.starter.infrastructure.persistence.repository.MemorySequenceRepository;
import com.ovelin.mall.id.generator.starter.infrastructure.persistence.repository.SequenceRepositoryImpl;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@MapperScan("com.ovelin.mall.id.generator.starter.infrastructure.persistence.mapper")
class SequenceRepositoryConfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = "ovelin.id-generator",
            name = "repository-type",
            havingValue = "database",
            matchIfMissing = true
    )
    SequenceRepository databaseSequenceRepository(
            IdSequenceMapper mapper,
            IdGeneratorProperties properties) {
        return new SequenceRepositoryImpl(mapper, properties);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "ovelin.id-generator",
            name = "repository-type",
            havingValue = "memory"
    )
    SequenceRepository memorySequenceRepository(
            IdGeneratorProperties properties) {
        return new MemorySequenceRepository(properties);
    }
}
