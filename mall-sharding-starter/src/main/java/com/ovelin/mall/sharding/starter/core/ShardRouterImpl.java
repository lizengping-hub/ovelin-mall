package com.ovelin.mall.sharding.starter.core;

import com.ovelin.mall.sharding.starter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ShardRouterImpl implements ShardRouter {

    private static final Logger log = LoggerFactory.getLogger(ShardRouterImpl.class);

    private final ShardingProperties properties;

    public ShardRouterImpl(ShardingProperties properties) {
        this.properties = properties;
    }

    @Override
    public ResolvedRoute route(ShardGroupKey groupKey) {
        ShardingProperties.ShardGroup shardGroup = properties.getShardGroup(groupKey); // Ensure the group exists
        if (shardGroup == null) {
            throw new IllegalArgumentException("Unknown shard group: " + groupKey);
        }
        // For non-sharded execution, we can default to the first route
        ShardingProperties.ShardRoute route = shardGroup.routes().getFirst();
        return resolveRoute(-1, route);
    }

    @Override
    public ResolvedRoute route(ShardGroupKey groupKey, ShardedId shardedId) {
        ShardingProperties.ShardGroup shardGroup = properties.getShardGroup(groupKey); // Ensure the group exists

        long logicalShardId = shardedId.getShardId();
        if (logicalShardId < 0 || logicalShardId > ShardedId.MAX_SHARD_ID) {
            throw new IllegalArgumentException("Shard ID out of range: " + logicalShardId);
        }
        // 逻辑 shardId 空间(0..SHARD_CAPACITY-1)折算为当前实际配置的物理分片索引(0..shardCount-1)
        int shardId = shardGroup.toPhysicalShardId(logicalShardId);

        List<ShardingProperties.ShardRoute> routes = shardGroup.routes();
        if (routes == null) {
            throw new IllegalArgumentException("Unknown shard domain: " + groupKey);
        }

        ResolvedRoute resolvedRoute = routes.stream()
                .filter(route -> shardId >= route.from() && shardId <= route.to())
                .findFirst()
                .map(route -> resolveRoute(shardId, route))
                .orElseThrow(() -> new IllegalStateException(
                        "No database route for domain " + groupKey + " and shard " + shardId));
        log.debug("Resolved shard route: domain={}, logicalShardId={}, physicalShardId={}, instance={}, database={}",
                groupKey, logicalShardId, shardId, resolvedRoute.instanceKey(), resolvedRoute.databaseName());
        return resolvedRoute;
    }

    private ResolvedRoute resolveRoute(
            int shardId,
            ShardingProperties.ShardRoute route) {
        if (!properties.instances().containsKey(route.instance())) {
            throw new IllegalStateException("Unknown database instance: " + route.instance());
        }
        return new ResolvedRoute(
                shardId,
                route.instance(),
                route.database());
    }

}
