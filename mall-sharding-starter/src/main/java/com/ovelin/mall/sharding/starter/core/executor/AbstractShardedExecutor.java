package com.ovelin.mall.sharding.starter.core.executor;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.sharding.starter.api.client.DatabaseClientManager;
import com.ovelin.mall.sharding.starter.api.excutor.ShardedExecutor;
import com.ovelin.mall.sharding.starter.api.router.ResolvedRoute;
import com.ovelin.mall.sharding.starter.api.router.ShardGroupKey;
import com.ovelin.mall.sharding.starter.api.router.ShardRouter;
import org.slf4j.Logger;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.BiFunction;

public abstract class AbstractShardedExecutor<C> implements ShardedExecutor<C> {
    private final ShardRouter shardRouter;

    protected final DatabaseClientManager databaseClientManager;

    protected abstract C getClient(ResolvedRoute route);
    protected abstract Logger logger();

    public AbstractShardedExecutor(ShardRouter shardRouter, DatabaseClientManager databaseClientManager) {
        this.shardRouter = shardRouter;
        this.databaseClientManager = databaseClientManager;
    }

    @Override
    public <R> R execute(ShardGroupKey groupKey, BiFunction<C, ResolvedRoute, R> action) {
        ResolvedRoute route = shardRouter.route(groupKey);
        logger().debug("Executing action for groupKey: {}, resolved route: {}", groupKey, route);
        return execute(route, action);
    }

    @Override
    public <R> R executeInTransaction(ShardGroupKey groupKey, BiFunction<C, ResolvedRoute, R> action) {
        ResolvedRoute route = shardRouter.route(groupKey);
        logger().debug("Executing action in transaction for groupKey: {}, resolved route: {}", groupKey, route);
        return executeInTransaction(route, action);
    }

    @Override
    public <R> R execute(ShardGroupKey groupKey, ShardedId shardedId, BiFunction<C, ResolvedRoute, R> action){
        ResolvedRoute route = shardRouter.route(groupKey, shardedId);
        logger().debug("Executing action for groupKey: {}, shardedId: {}, resolved route: {}", groupKey, shardedId, route);
        return execute(route, action);
    }

    @Override
    public <R> R executeInTransaction(ShardGroupKey groupKey, ShardedId shardedId, BiFunction<C, ResolvedRoute, R> action){
        ResolvedRoute route = shardRouter.route(groupKey, shardedId);
        logger().debug("Executing action in transaction for groupKey: {}, shardedId: {}, resolved route: {}", groupKey, shardedId, route);
        return executeInTransaction(route, action);
    }

    @Override
    public <R> R execute(ResolvedRoute route, BiFunction<C, ResolvedRoute, R> action) {
        C client = getClient(route);
        return action.apply(client, route);
    }

    @Override
    public <R> R executeInTransaction(ResolvedRoute route, BiFunction<C, ResolvedRoute, R> action) {
        C client = getClient(route);
        TransactionTemplate transactionTemplate = databaseClientManager.getTransactionTemplate(route.instanceKey());
        return transactionTemplate.execute(_ -> action.apply(client, route));
    }
}
