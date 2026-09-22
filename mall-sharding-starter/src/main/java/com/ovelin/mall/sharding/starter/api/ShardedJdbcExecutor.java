package com.ovelin.mall.sharding.starter.api;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.function.BiFunction;

public interface ShardedJdbcExecutor {

    /**
     * 只解析默认路由，不执行数据库操作。
     *
     * <p>不传分片 ID 时不进行分片计算，返回指定分片组对应的默认数据源路由。
     * 适用于业务只需要获取路由信息的场景，例如构造缓存 Key、日志埋点，
     * 或判断目标数据源等。</p>
     */
    ResolvedRoute resolveRoute(ShardGroupKey groupKey);

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
            BiFunction<JdbcTemplate, ResolvedRoute, R> action);

    /**
     * 不进行分片，使用指定分片组的默认数据源执行数据库操作。
     *
     * <p>向 action 同时提供 JdbcTemplate 和 ResolvedRoute。
     * 在目标数据库内开启单库事务，仅保证当前数据库内多条 SQL 的原子性，
     * 不支持跨库事务。</p>
     */
    <R> R executeInTransaction(
            ShardGroupKey groupKey,
            BiFunction<JdbcTemplate, ResolvedRoute, R> action);

    /**
     * 使用已解析的路由执行数据库操作。
     *
     * <p>不显式开启事务，数据库连接使用连接池的默认 autocommit 行为。
     * 适用于调用方已经提前完成路由计算，并希望复用该路由执行操作的场景。</p>
     */
    <R> R execute(
            ResolvedRoute route,
            BiFunction<JdbcTemplate, ResolvedRoute, R> action);

    /**
     * 使用已解析的路由执行数据库操作，并在目标数据库内开启单库事务。
     *
     * <p>仅保证当前数据库内多条 SQL 的原子性，不支持跨库事务。</p>
     */
    <R> R executeInTransaction(
            ResolvedRoute route,
            BiFunction<JdbcTemplate, ResolvedRoute, R> action);


    /**
     * 根据分片组和 ID 布局计算目标分片路由，不执行数据库操作。
     */
    ResolvedRoute resolveRoute(
            ShardGroupKey groupKey,
            ShardedId idLayout);

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
            BiFunction<JdbcTemplate, ResolvedRoute, R> action);

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
            BiFunction<JdbcTemplate, ResolvedRoute, R> action);
}