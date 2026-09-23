package com.ovelin.mall.sharding.starter.autoconfigure;

import com.ovelin.mall.sharding.starter.api.*;
import com.ovelin.mall.sharding.starter.core.DatabaseClientManagerImpl;
import com.ovelin.mall.sharding.starter.core.ShardRouterImpl;
import com.ovelin.mall.sharding.starter.core.ShardedJdbcExecutorImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ShardingProperties.class)
public class ShardingAutoConfiguration {

    @Bean
    public ShardRouter shardRouter(ShardingProperties properties) {
        return new ShardRouterImpl(properties);
    }

    @Bean
    public DatabaseClientManager databaseClientManager(ShardingProperties properties) {
        return new DatabaseClientManagerImpl(properties);
    }

    @Bean
    public ShardedJdbcExecutor shardedJdbcExecutor(
            DatabaseClientManager databaseClientManager,
            ShardRouter shardRouter) {
        return new ShardedJdbcExecutorImpl(databaseClientManager, shardRouter);
    }

    @Bean
    public ShardedMyBatisExecutor shardedMyBatisExecutor(
            DatabaseClientManager databaseClientManager,
            ShardRouter shardRouter) {
        return new com.ovelin.mall.sharding.starter.core.ShardedMyBatisExecutorImpl(databaseClientManager, (ShardRouterImpl) shardRouter);
    }

}