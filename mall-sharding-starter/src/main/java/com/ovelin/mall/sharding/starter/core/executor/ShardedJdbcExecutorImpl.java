package com.ovelin.mall.sharding.starter.core.executor;

import com.ovelin.mall.sharding.starter.api.client.DatabaseClientManager;
import com.ovelin.mall.sharding.starter.api.router.ResolvedRoute;
import com.ovelin.mall.sharding.starter.api.router.ShardRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.jdbc.core.JdbcTemplate;

public class ShardedJdbcExecutorImpl extends AbstractShardedExecutor<JdbcTemplate> {
    private static final Logger log = LoggerFactory.getLogger(ShardedJdbcExecutorImpl.class);
    public ShardedJdbcExecutorImpl(DatabaseClientManager databaseClientManager, ShardRouter shardRouter) {
        super(shardRouter, databaseClientManager);
    }

    @Override
    protected JdbcTemplate getClient(ResolvedRoute route) {
        return databaseClientManager.getJdbcTemplate(route.instanceKey());
    }
}
