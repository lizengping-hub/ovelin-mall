package com.ovelin.mall.sharding.starter.core;

import com.ovelin.mall.common.sharding.core.ov.ShardId;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.sharding.starter.api.router.ResolvedRoute;
import com.ovelin.mall.sharding.starter.api.router.ShardGroupKey;
import com.ovelin.mall.sharding.starter.api.router.ShardingProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShardRouterImplTest {

    private static final ShardGroupKey USER_GROUP = new ShardGroupKey("user");

    private static final ShardingProperties.Instance INSTANCE_0 =
            new ShardingProperties.Instance("host-0", 3306, "root", "root", 10);
    private static final ShardingProperties.Instance INSTANCE_1 =
            new ShardingProperties.Instance("host-1", 3306, "root", "root", 10);

    @Test
    void routesLogicalShardIdToCorrectPhysicalDatabase() {
        // shardCount=2，SHARD_CAPACITY/2 个逻辑 shardId 对应一个物理分片
        ShardRouterImpl router = routerWith(2,
                new ShardingProperties.ShardRoute(0, 0, "mysql-0", "user_database_0"),
                new ShardingProperties.ShardRoute(1, 1, "mysql-1", "user_database_1"));

        // 逻辑空间的前一半应落在物理分片 0
        ResolvedRoute low = router.route(USER_GROUP, ShardedId.of(0, 0L));
        assertThat(low.databaseName()).isEqualTo("user_database_0");
        assertThat(low.instanceKey()).isEqualTo("mysql-0");

        // 逻辑空间的后一半应落在物理分片 1
        ResolvedRoute high = router.route(USER_GROUP, ShardedId.of(ShardId.SHARD_CAPACITY - 1, 0L));
        assertThat(high.databaseName()).isEqualTo("user_database_1");
        assertThat(high.instanceKey()).isEqualTo("mysql-1");
    }

    @Test
    void sameLogicalShardIdAlwaysResolvesToSamePhysicalRoute() {
        ShardRouterImpl router = routerWith(256, fullRangeRoute(256, "mysql-0", "user_database"));
        int shardId = 12345;

        ResolvedRoute first = router.route(USER_GROUP, ShardedId.of(shardId, 1L));
        ResolvedRoute second = router.route(USER_GROUP, ShardedId.of(shardId, 999L));

        assertThat(first.shardId()).isEqualTo(second.shardId());
        assertThat(first.databaseName()).isEqualTo(second.databaseName());
    }

    @Test
    void routeThrowsWhenGroupKeyIsUnknown() {
        ShardRouterImpl router = routerWith(256, fullRangeRoute(256, "mysql-0", "user_database"));

        assertThatThrownBy(() -> router.route(new ShardGroupKey("does-not-exist"), ShardedId.of(0, 0L)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void routeThrowsWhenInstanceIsUnknown() {
        ShardingProperties properties = new ShardingProperties(
                Map.of("mysql-0", INSTANCE_0),
                Map.of(USER_GROUP, new ShardingProperties.ShardGroup(
                        "user", 1, List.of(new ShardingProperties.ShardRoute(0, 0, "missing-instance", "user_database")))));
        ShardRouterImpl router = new ShardRouterImpl(properties);

        assertThatThrownBy(() -> router.route(USER_GROUP, ShardedId.of(0, 0L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unknown database instance");
    }

    private ShardRouterImpl routerWith(int shardCount, ShardingProperties.ShardRoute... routes) {
        ShardingProperties properties = new ShardingProperties(
                Map.of("mysql-0", INSTANCE_0, "mysql-1", INSTANCE_1),
                Map.of(USER_GROUP, new ShardingProperties.ShardGroup("user", shardCount, List.of(routes))));
        return new ShardRouterImpl(properties);
    }

    private ShardingProperties.ShardRoute fullRangeRoute(int shardCount, String instance, String database) {
        return new ShardingProperties.ShardRoute(0, shardCount - 1, instance, database);
    }
}
