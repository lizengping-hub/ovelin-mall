package com.ovelin.mall.sharding.starter.api;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.function.BiFunction;

public interface ShardedJdbcExecutor {

    /**
     * 只路由到对应库并执行，不显式开启事务，走连接池默认的 autocommit。
     * 适用于单条 SQL 或本身不需要跨多条语句保证原子性的场景。
     */
    public <R> R execute(
            ShardGroupKey groupKey,
            ShardedId idLayout,
            BiFunction<JdbcTemplate, ResolvedRoute, R> action);

    /**
     * 路由到对应库，并显式包裹在该库的单库事务中执行。
     * 仅保证单库范围内多条 SQL 的原子性，不支持跨库事务。
     */
    public <R> R executeInTransaction(
            ShardGroupKey groupKey,
            ShardedId idLayout,
            BiFunction<JdbcTemplate, ResolvedRoute, R> action);



    /**
     * 只做路由计算，不执行任何数据库操作。
     * 用于业务只需要知道分片结果（比如构造缓存 key、日志埋点，或判断多个 key 是否落在同一物理库）而不需要访问数据库的场景。
     */
    public ResolvedRoute resolveRoute(ShardGroupKey groupKey, ShardedId idLayout);

    /**
     * 只路由到对应库并执行，不显式开启事务，走连接池默认的 autocommit。
     * 适用于单条 SQL 或本身不需要跨多条语句保证原子性的场景。
     */
    public <R> R execute(
            ResolvedRoute route,
            BiFunction<JdbcTemplate, ResolvedRoute, R> action);

    /**
     * 路由到对应库，并显式包裹在该库的单库事务中执行。
     * 仅保证单库范围内多条 SQL 的原子性，不支持跨库事务。
     */
    public <R> R executeInTransaction(
            ResolvedRoute route,
            BiFunction<JdbcTemplate, ResolvedRoute, R> action);
}
