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
    public ShardedJdbcExecutorImpl shardTransactionExecutor(
            DatabaseClientManager databaseClientManager,
            ShardRouter shardRouter) {
        return new ShardedJdbcExecutorImpl(databaseClientManager, shardRouter);
    }

    @Bean
    public ShardRouter shardRouter(ShardingProperties properties) {
        return new ShardRouter(properties);
    }

    @Bean
    public DatabaseClientManager databaseClientManager(ShardingProperties properties) {
        return new DatabaseClientManager(properties);
    }
}