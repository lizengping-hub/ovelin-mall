package com.ovelin.mall.sharding.starter.autoconfigure;

import com.ovelin.mall.sharding.starter.api.*;
import com.ovelin.mall.sharding.starter.api.client.DatabaseClientManager;
import com.ovelin.mall.sharding.starter.api.excutor.ShardedJdbcExecutor;
import com.ovelin.mall.sharding.starter.api.excutor.ShardedMyBatisExecutor;
import com.ovelin.mall.sharding.starter.api.router.ShardRouter;
import com.ovelin.mall.sharding.starter.api.router.ShardingProperties;
import com.ovelin.mall.sharding.starter.core.*;
import com.ovelin.mall.sharding.starter.core.client.DatabaseClientManagerImpl;
import com.ovelin.mall.sharding.starter.core.executor.ShardedJdbcExecutorImpl;
import com.ovelin.mall.sharding.starter.core.executor.ShardedMyBatisExecutorImpl;
import com.ovelin.mall.sharding.starter.core.executor.ShardedMyBatisPlusExecutorImpl;
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
        return new ShardedMyBatisExecutorImpl(databaseClientManager, (ShardRouterImpl) shardRouter);
    }

    @Bean
    public ShardedMyBatisPlusExecutor shardedMyBatisPlusExecutor(
            DatabaseClientManager databaseClientManager,
            ShardRouter shardRouter) {
        return new ShardedMyBatisPlusExecutorImpl(databaseClientManager, (ShardRouterImpl) shardRouter);
    }

}