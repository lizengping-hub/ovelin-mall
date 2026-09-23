package com.ovelin.mall.sharding.starter.core;

import com.ovelin.mall.sharding.starter.api.*;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.BiFunction;

public class ShardedMyBatisExecutorImpl implements ShardedMyBatisExecutor {
    private final ShardRouter shardRouter;
    private final DatabaseClientManager databaseClientManager;

    public ShardedMyBatisExecutorImpl(DatabaseClientManager databaseClientManager, ShardRouter shardRouter) {
        this.databaseClientManager = databaseClientManager;
        this.shardRouter = shardRouter;
    }

    @Override
    public <R> R execute(ShardGroupKey groupKey, BiFunction<MyBatisClient, ResolvedRoute, R> action) {
        ResolvedRoute route = shardRouter.route(groupKey);
        return execute(route, action);
    }

    @Override
    public <R> R executeInTransaction(ShardGroupKey groupKey, BiFunction<MyBatisClient, ResolvedRoute, R> action) {
        ResolvedRoute route = shardRouter.route(groupKey);
        return executeInTransaction(route, action);
    }
    @Override
    public <R> R execute(ShardGroupKey groupKey, ShardedId shardedId, BiFunction<MyBatisClient, ResolvedRoute, R> action) {
        ResolvedRoute route = shardRouter.route(groupKey, shardedId);
        return execute(route, action);
    }

    @Override
    public <R> R executeInTransaction(ShardGroupKey groupKey, ShardedId shardedId, BiFunction<MyBatisClient, ResolvedRoute, R> action) {
        ResolvedRoute route = shardRouter.route(groupKey, shardedId);
        return executeInTransaction(route, action);
    }


    @Override
    public <R> R execute(ResolvedRoute route, BiFunction<MyBatisClient, ResolvedRoute, R> action) {
        MyBatisClient client = databaseClientManager.getMyBatisClient(route.instanceKey());
        return action.apply(client, route);
    }

    @Override
    public <R> R executeInTransaction(ResolvedRoute route, BiFunction<MyBatisClient, ResolvedRoute, R> action) {
        MyBatisClient client = databaseClientManager.getMyBatisClient(route.instanceKey());
        TransactionTemplate transactionTemplate = databaseClientManager.getTransactionTemplate(route.instanceKey());
        return transactionTemplate.execute(_ -> action.apply(client, route));
    }
}
