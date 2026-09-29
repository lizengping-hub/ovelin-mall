package com.ovelin.mall.sharding.starter.api.excutor.facade;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.sharding.starter.api.router.ResolvedRoute;

import java.util.function.BiFunction;
/**
 * 业务内部使用 ShardGroupExecutor 来执行分片组相关的数据库操作，业务方需对此接口进行实现
 *
 *
 * @param <C> 具体的数据库访问客户端类型，如 JdbcTemplate、MyBatisClient、MyBatisPlusClient。
 */
public interface ShardGroupExecutor<C> {
    /** 不分片，使用分片组默认数据源，不开事务。 */
    <R> R execute(BiFunction<C, ResolvedRoute, R> action);

    /** 不分片，使用分片组默认数据源，开单库事务。 */
    <R> R executeInTransaction(BiFunction<C, ResolvedRoute, R> action);
    /** ID 布局计算路由，不开事务。 */
    <R> R execute(ShardedId shardedId, BiFunction<C, ResolvedRoute, R> action);

    /** ID 布局计算路由，开单库事务。 */
    <R> R executeInTransaction(ShardedId shardedId, BiFunction<C, ResolvedRoute, R> action);
}