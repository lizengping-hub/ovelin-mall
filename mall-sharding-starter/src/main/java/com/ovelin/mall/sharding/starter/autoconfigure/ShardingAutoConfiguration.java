package com.ovelin.mall.sharding.starter.autoconfigure;

import com.ovelin.mall.sharding.starter.core.DatabaseClientManager;
import com.ovelin.mall.sharding.starter.core.ShardRouter;
import com.ovelin.mall.sharding.starter.api.ShardingProperties;
import com.ovelin.mall.sharding.starter.core.ShardedJdbcExecutorImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ShardingProperties.class)
public class ShardingAutoConfiguration {
    @Bean
    public ShardedJdbcExecutorImpl shardTransactionExecutor(ShardingProperties properties) {
        return new ShardedJdbcExecutorImpl(new DatabaseClientManager(properties), new ShardRouter(properties));
    }
}