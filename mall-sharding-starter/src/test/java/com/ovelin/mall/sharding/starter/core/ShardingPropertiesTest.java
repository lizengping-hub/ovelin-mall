package com.ovelin.mall.sharding.starter.core;

import com.ovelin.mall.sharding.starter.api.ShardedId;
import com.ovelin.mall.sharding.starter.api.router.ShardGroupKey;
import com.ovelin.mall.sharding.starter.api.router.ShardingProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShardingPropertiesTest {

    private static final ShardingProperties.Instance INSTANCE =
            new ShardingProperties.Instance("localhost", 3306, "root", "root", 10);

    @Test
    void validatePassesForWellFormedConfiguration() {
        ShardingProperties properties = withGroup(group(256, route(0, 255, "mysql-0", "user_database")));
        properties.validate();
    }

    @Test
    void validateFailsWhenShardCountIsNotPowerOfTwo() {
        ShardingProperties properties = withGroup(group(3, route(0, 2, "mysql-0", "user_database")));
        assertThatThrownBy(properties::validate)
                .isInstanceOf(ShardingConfigurationException.class)
                .hasMessageContaining("power of two");
    }

    @Test
    void validateFailsWhenShardCountDoesNotDivideShardCapacity() {
        // shardCount 大于 SHARD_CAPACITY 时依然可能是 2 的幂（例如 SHARD_CAPACITY * 2），
        // 但无法整除 SHARD_CAPACITY，用来验证兜底的整除校验（防止未来 SHARD_BITS 调整后出现回归）。
        int oversized = ShardedId.SHARD_CAPACITY * 2; // 仍是 2 的幂，但不能整除 SHARD_CAPACITY
        ShardingProperties.ShardGroup group = new ShardingProperties.ShardGroup(
                "user", oversized, List.of(route(0, oversized - 1, "mysql-0", "user_database")));
        assertThatThrownBy(group::validate)
                .isInstanceOf(ShardingConfigurationException.class)
                .hasMessageContaining("evenly divide");
    }

    @Test
    void validateFailsWhenRouteInstanceIsUnknown() {
        ShardingProperties properties = withGroup(group(256, route(0, 255, "unknown-instance", "user_database")));
        assertThatThrownBy(properties::validate)
                .isInstanceOf(ShardingConfigurationException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    void validateFailsWhenRoutesHaveGap() {
        ShardingProperties properties = withGroup(group(256,
                route(0, 100, "mysql-0", "user_database"),
                route(102, 255, "mysql-0", "user_database")));
        assertThatThrownBy(properties::validate)
                .isInstanceOf(ShardingConfigurationException.class)
                .hasMessageContaining("ascending order");
    }

    @Test
    void validateFailsWhenRoutesDoNotCoverAllShards() {
        ShardingProperties properties = withGroup(group(256,
                route(0, 200, "mysql-0", "user_database")));
        assertThatThrownBy(properties::validate)
                .isInstanceOf(ShardingConfigurationException.class)
                .hasMessageContaining("cover all shards");
    }

    @Test
    void shardsPerPhysicalAndToPhysicalShardIdAreConsistent() {
        ShardingProperties.ShardGroup group = group(256, route(0, 255, "mysql-0", "user_database"));
        int ratio = group.shardsPerPhysical();
        assertThat(ratio).isEqualTo(ShardedId.SHARD_CAPACITY / 256);

        assertThat(group.toPhysicalShardId(0)).isZero();
        assertThat(group.toPhysicalShardId(ratio - 1L)).isZero();
        assertThat(group.toPhysicalShardId(ratio)).isEqualTo(1);
        assertThat(group.toPhysicalShardId(ShardedId.MAX_SHARD_ID)).isEqualTo(255);
    }

    private ShardingProperties withGroup(ShardingProperties.ShardGroup group) {
        return new ShardingProperties(
                Map.of("mysql-0", INSTANCE),
                Map.of(new ShardGroupKey("user"), group));
    }

    private ShardingProperties.ShardGroup group(int shardCount, ShardingProperties.ShardRoute... routes) {
        return new ShardingProperties.ShardGroup("user", shardCount, List.of(routes));
    }

    private ShardingProperties.ShardRoute route(int from, int to, String instance, String database) {
        return new ShardingProperties.ShardRoute(from, to, instance, database);
    }
}
