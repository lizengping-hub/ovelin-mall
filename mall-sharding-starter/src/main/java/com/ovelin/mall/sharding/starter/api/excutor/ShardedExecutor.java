package com.ovelin.mall.sharding.starter.api.excutor;

import com.ovelin.mall.sharding.starter.api.router.ResolvedRoute;
import com.ovelin.mall.sharding.starter.api.router.ShardGroupKey;
import com.ovelin.mall.sharding.starter.api.ShardedId;

import java.util.function.BiFunction;

/**
 * 分片执行器的通用抽象。
 *
 * @param <C> 具体的数据库访问客户端类型，如 JdbcTemplate、MyBatisClient、MyBatisPlusClient。
 */
public interface ShardedExecutor<C> {

    /** 不分片，使用分片组默认数据源，不开事务。 */
    <R> R execute(ShardGroupKey groupKey, BiFunction<C, ResolvedRoute, R> action);

    /** 不分片，使用分片组默认数据源，开单库事务。 */
    <R> R executeInTransaction(ShardGroupKey groupKey, BiFunction<C, ResolvedRoute, R> action);

    /** 使用已解析路由，不开事务。 */
    <R> R execute(ResolvedRoute route, BiFunction<C, ResolvedRoute, R> action);

    /** 使用已解析路由，开单库事务。 */
    <R> R executeInTransaction(ResolvedRoute route, BiFunction<C, ResolvedRoute, R> action);

    /** 根据分片组 + ID 布局计算路由，不开事务。 */
    <R> R execute(ShardGroupKey groupKey, ShardedId shardedId, BiFunction<C, ResolvedRoute, R> action);

    /** 根据分片组 + ID 布局计算路由，开单库事务。 */
    <R> R executeInTransaction(ShardGroupKey groupKey, ShardedId shardedId, BiFunction<C, ResolvedRoute, R> action);
}
