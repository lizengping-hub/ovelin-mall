package com.ovelin.mall.id.generator.starter.autoconfigure;

import com.ovelin.mall.id.generator.starter.domain.repository.SequenceRepository;
import com.ovelin.mall.id.generator.starter.infrastructure.persistence.mapper.IdSequenceMapper;
import com.ovelin.mall.id.generator.starter.infrastructure.persistence.repository.IdSequenceRepositoryImpl;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@MapperScan("com.ovelin.mall.id.generator.starter.infrastructure.persistence.mapper")
class SequenceRepositoryConfiguration {
    @Bean
    SequenceRepository sequenceRepository(IdSequenceMapper mapper, IdGeneratorProperties properties) {
        return new IdSequenceRepositoryImpl(mapper, properties);
    }
}
