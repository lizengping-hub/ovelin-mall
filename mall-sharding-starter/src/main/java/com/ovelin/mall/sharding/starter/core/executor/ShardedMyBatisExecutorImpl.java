package com.ovelin.mall.sharding.starter.core.executor;

import com.ovelin.mall.sharding.starter.api.client.DatabaseClientManager;
import com.ovelin.mall.sharding.starter.api.router.ResolvedRoute;
import com.ovelin.mall.sharding.starter.api.router.ShardRouter;
import com.ovelin.mall.sharding.starter.core.client.MyBatisClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShardedMyBatisExecutorImpl extends AbstractShardedExecutor<MyBatisClient> {
    private static final Logger log = LoggerFactory.getLogger(ShardedMyBatisExecutorImpl.class);
    public ShardedMyBatisExecutorImpl(DatabaseClientManager databaseClientManager, ShardRouter shardRouter) {
        super(shardRouter, databaseClientManager);
    }

    @Override
    protected MyBatisClient getClient(ResolvedRoute route) {
        return databaseClientManager.getMyBatisClient(route.instanceKey());
    }
}
