package com.ovelin.mall.sharding.starter.api.excutor.facade;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.sharding.starter.api.excutor.ShardedExecutor;
import com.ovelin.mall.sharding.starter.api.router.ResolvedRoute;
import com.ovelin.mall.sharding.starter.api.router.ShardGroupKey;

import java.util.function.BiFunction;

public abstract class BaseShardGroupExecutor<C> implements ShardGroupExecutor<C>{

    protected abstract ShardGroupKey getShardGroupKey();
    private final ShardedExecutor<C> executor;

    public BaseShardGroupExecutor(ShardedExecutor<C> executor) {
        this.executor = executor;
    }

    @Override
    public <R> R execute(BiFunction<C, ResolvedRoute, R> action) {
        return executor.execute(getShardGroupKey(), action);
    }

    @Override
    public <R> R executeInTransaction(BiFunction<C, ResolvedRoute, R> action) {
        return executor.executeInTransaction(getShardGroupKey(), action);
    }

    @Override
    public <R> R execute(ShardedId shardedId, BiFunction<C, ResolvedRoute, R> action) {
        return executor.execute(getShardGroupKey(), shardedId, action);
    }

    @Override
    public <R> R executeInTransaction(ShardedId shardedId, BiFunction<C, ResolvedRoute, R> action) {
        return executor.executeInTransaction(getShardGroupKey(), shardedId, action);
    }
}
