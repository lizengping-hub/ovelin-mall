package com.ovelin.mall.sharding.starter.core;

import com.ovelin.mall.sharding.starter.api.ShardedId;
import com.ovelin.mall.sharding.starter.api.ResolvedRoute;
import com.ovelin.mall.sharding.starter.api.ShardGroupKey;
import com.ovelin.mall.sharding.starter.api.ShardedJdbcExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.BiFunction;
import java.util.function.Function;

public class ShardedJdbcExecutorImpl implements ShardedJdbcExecutor {
    private final ShardRouter shardRouter;
    private static final Logger log = LoggerFactory.getLogger(ShardedJdbcExecutorImpl.class);

    private final DatabaseClientManager databaseClientManager;

    public ShardedJdbcExecutorImpl(DatabaseClientManager databaseClientManager, ShardRouter shardRouter) {
        this.databaseClientManager = databaseClientManager;
        this.shardRouter = shardRouter;
    }

    @Override
    public ResolvedRoute resolveRoute(ShardGroupKey groupKey, ShardedId idLayout) {
        return shardRouter.route(groupKey, idLayout);
    }

    @Override
    public <R> R execute(ShardGroupKey groupKey, ShardedId idLayout, BiFunction<JdbcTemplate, ResolvedRoute, R> action) {
        ResolvedRoute route = resolveRoute(groupKey, idLayout);
        return execute(route, action);
    }

    @Override
    public <R> R executeInTransaction(ShardGroupKey groupKey, ShardedId idLayout, BiFunction<JdbcTemplate, ResolvedRoute, R> action) {
        ResolvedRoute route = resolveRoute(groupKey, idLayout);
        return executeInTransaction(route, action);
    }


    @Override
    public ResolvedRoute resolveRoute(ShardGroupKey groupKey) {
        return shardRouter.route(groupKey);
    }


    @Override
    public <R> R execute(ShardGroupKey groupKey, BiFunction<JdbcTemplate, ResolvedRoute, R> action) {
        ResolvedRoute route = resolveRoute(groupKey);
        return execute(route, action);
    }

    @Override
    public <R> R executeInTransaction(ShardGroupKey groupKey, BiFunction<JdbcTemplate, ResolvedRoute, R> action) {
        ResolvedRoute route = resolveRoute(groupKey);
        return executeInTransaction(route, action);
    }


    @Override
    public <R> R execute(ResolvedRoute route, BiFunction<JdbcTemplate, ResolvedRoute, R> action) {
        JdbcTemplate template = databaseClientManager.getJdbcTemplate(route.instanceKey());
        return action.apply(template, route);
    }

    @Override
    public <R> R executeInTransaction(ResolvedRoute route, BiFunction<JdbcTemplate, ResolvedRoute, R> action) {
        JdbcTemplate template = databaseClientManager.getJdbcTemplate(route.instanceKey());
        TransactionTemplate transactionTemplate = databaseClientManager.getTransactionTemplate(route.instanceKey());
        return transactionTemplate.execute(_ -> action.apply(template, route));
    }


}
