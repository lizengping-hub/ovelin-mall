package com.ovelin.mall.sharding.starter.core.executor;

import com.ovelin.mall.sharding.starter.api.client.DatabaseClientManager;
import com.ovelin.mall.sharding.starter.api.router.ResolvedRoute;
import com.ovelin.mall.sharding.starter.api.router.ShardRouter;
import com.ovelin.mall.sharding.starter.core.client.MyBatisPlusClient;
import com.ovelin.mall.sharding.starter.core.client.TableRouteContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiFunction;

public class ShardedMyBatisPlusExecutorImpl extends AbstractShardedExecutor<MyBatisPlusClient> {
    private static final Logger log = LoggerFactory.getLogger(ShardedMyBatisPlusExecutorImpl.class);
    public ShardedMyBatisPlusExecutorImpl(DatabaseClientManager databaseClientManager, ShardRouter shardRouter) {
        super(shardRouter, databaseClientManager);
    }

    @Override
    protected MyBatisPlusClient getClient(ResolvedRoute route) {
        return databaseClientManager.getMyBatisPlusClient(route.instanceKey());
    }
    @Override
    public <R> R execute(ResolvedRoute route, BiFunction<MyBatisPlusClient, ResolvedRoute, R> action) {
        try (var ignored = TableRouteContext.use(route)) {
            return super.execute(route, action);
        }
    }

    @Override
    public <R> R executeInTransaction(ResolvedRoute route, BiFunction<MyBatisPlusClient, ResolvedRoute, R> action) {
        try (var ignored = TableRouteContext.use(route)) {
            return super.executeInTransaction(route, action);
        }
    }
}

