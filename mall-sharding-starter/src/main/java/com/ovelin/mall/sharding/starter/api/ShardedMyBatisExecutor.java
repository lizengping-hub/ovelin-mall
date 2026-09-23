package com.ovelin.mall.sharding.starter.api;

import com.ovelin.mall.sharding.starter.core.MyBatisClient;

import java.util.function.BiFunction;

public interface ShardedMyBatisExecutor {

    /**
     * 不进行分片，使用指定分片组的默认数据源执行数据库操作。
     *
     * <p>向 action 同时提供 JdbcTemplate 和 ResolvedRoute，
     * 方便业务逻辑使用路由信息进行日志记录、监控埋点等操作。
     *
     * <p>不显式开启事务，数据库连接使用连接池的默认 autocommit 行为。
     */
    <R> R execute(
            ShardGroupKey groupKey,
            BiFunction<MyBatisClient, ResolvedRoute, R> action);


    /**
     * 不进行分片，使用指定分片组的默认数据源执行数据库操作。
     *
     * <p>向 action 同时提供 JdbcTemplate 和 ResolvedRoute。
     * 在目标数据库内开启单库事务，仅保证当前数据库内多条 SQL 的原子性，
     * 不支持跨库事务。</p>
     */
    <R> R executeInTransaction(
            ShardGroupKey groupKey,
            BiFunction<MyBatisClient, ResolvedRoute, R> action);

    /**
     * 使用已解析的路由执行数据库操作。
     *
     * <p>不显式开启事务，数据库连接使用连接池的默认 autocommit 行为。
     * 适用于调用方已经提前完成路由计算，并希望复用该路由执行操作的场景。</p>
     */
    <R> R execute(
            ResolvedRoute route,
            BiFunction<MyBatisClient, ResolvedRoute, R> action);

    /**
     * 使用已解析的路由执行数据库操作，并在目标数据库内开启单库事务。
     *
     * <p>仅保证当前数据库内多条 SQL 的原子性，不支持跨库事务。</p>
     */
    <R> R executeInTransaction(
            ResolvedRoute route,
            BiFunction<MyBatisClient, ResolvedRoute, R> action);


    /**
     * 根据分片组和 ID 布局计算目标分片，
     * 路由到对应数据库后执行数据库操作。
     *
     * <p>向 action 同时提供 JdbcTemplate 和 ResolvedRoute，
     * 方便业务逻辑使用路由信息进行日志记录、监控埋点等操作。
     *
     * <p>不显式开启事务，数据库连接使用连接池的默认 autocommit 行为。
     */
    <R> R execute(
            ShardGroupKey groupKey,
            ShardedId idLayout,
            BiFunction<MyBatisClient, ResolvedRoute, R> action);

    /**
     * 根据分片组和 ID 布局计算目标分片，
     * 路由到对应数据库后执行数据库操作。
     *
     * <p>向 action 同时提供 JdbcTemplate 和 ResolvedRoute。
     * 在目标数据库内开启单库事务，仅保证当前数据库内多条 SQL 的原子性，
     * 不支持跨库事务。</p>
     */
    <R> R executeInTransaction(
            ShardGroupKey groupKey,
            ShardedId idLayout,
            BiFunction<MyBatisClient, ResolvedRoute, R> action);
}
