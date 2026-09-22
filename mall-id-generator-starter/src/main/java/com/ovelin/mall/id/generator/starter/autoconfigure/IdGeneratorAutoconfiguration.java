package com.ovelin.mall.id.generator.starter.autoconfigure;

import com.ovelin.mall.id.generator.starter.IdGeneratorConstant;
import com.ovelin.mall.id.generator.starter.domain.port.SequenceRepository;
import com.ovelin.mall.id.generator.starter.infrastructure.persistence.SequenceRepositoryImpl;
import com.ovelin.mall.sharding.starter.api.ShardedJdbcExecutor;
import com.ovelin.mall.sharding.starter.api.ShardingProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ShardingProperties.class)
public class IdGeneratorAutoconfiguration {

    private final ShardingProperties shardingProperties;

    public IdGeneratorAutoconfiguration(ShardingProperties shardingProperties) {
        this.shardingProperties = shardingProperties;
    }

    @PostConstruct
    public void validateShardingProperties() {
        if (shardingProperties.getShardGroup(IdGeneratorConstant.SHARD_GROUP_KEY) == null) {
            throw new IllegalStateException("Shard group not found: " + IdGeneratorConstant.SHARD_GROUP_KEY);
        }
    }

    @Bean
    public SequenceRepository sequenceRepository(ShardedJdbcExecutor executor) {
        return new SequenceRepositoryImpl(executor);
    }
}
