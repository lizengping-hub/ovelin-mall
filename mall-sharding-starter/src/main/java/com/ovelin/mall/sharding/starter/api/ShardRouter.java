package com.ovelin.mall.sharding.starter.api;

public interface ShardRouter {
    /**
     * 只解析默认路由，不执行数据库操作。
     *
     * <p>不传分片 ID 时不进行分片计算，返回指定分片组对应的默认数据源路由。
     * 适用于业务只需要获取路由信息的场景，例如构造缓存 Key、日志埋点，
     * 或判断目标数据源等。</p>
     */
    public ResolvedRoute route(ShardGroupKey groupKey);

    /**
     * 根据分片组和 ID 布局计算目标分片路由，不执行数据库操作。
     */
    public ResolvedRoute route(ShardGroupKey groupKey, ShardedId idLayout);

}
