package com.ovelin.mall.sharding.starter.api.router;

import com.ovelin.mall.sharding.starter.api.ShardedId;
import com.ovelin.mall.sharding.starter.core.ShardingConfigurationException;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "ovelin.sharding")
@Validated
public record ShardingProperties(
        Map<String, @Valid Instance> instances ,
        Map<ShardGroupKey, @Valid ShardGroup> shardRoutes
) {
    public ShardGroup getShardGroup(ShardGroupKey key) {
        return shardRoutes.get(key);
    }
    @PostConstruct
    public void validate() {
        if (instances == null) {
            throw new ShardingConfigurationException("ovelin.sharding.instances must be configured");
        }
        if (shardRoutes == null) {
            throw new ShardingConfigurationException("ovelin.sharding.shard-routes must be configured");
        }
        for (ShardGroup group : shardRoutes.values()) {
            group.validate();
            int lastTo = -1;
            List<ShardRoute> sorted = group.routes().stream()
                    .sorted(Comparator.comparingInt(ShardRoute::from))
                    .toList();
            for (ShardRoute route : sorted) {
                if (!instances.containsKey(route.instance())){
                    throw new ShardingConfigurationException("Shard route instance " + route.instance() + " does not exist");
                }
                if (route.from() != lastTo + 1) {
                    throw new ShardingConfigurationException("Shard route 'from' must be in ascending order");
                }
                if (route.to() < route.from()) {
                    throw new ShardingConfigurationException("Shard route 'to' must be greater than or equal to 'from'");        }
                lastTo = route.to();
            }
            if (lastTo != group.shardCount() - 1) {
                throw new ShardingConfigurationException("Shard route 'to' must cover all shards");
            }
        }
    }
    public record Instance(
        @NotNull
        @NotBlank
        String host,
        @Min(value = 1, message = "Port must be at least 1")
        @Max(value = 65535, message = "Port must be at most 65535")
        @DefaultValue("3306")
        int port,
        @NotNull
        @NotBlank
        String username,
        String password,
        @Min(value = 1, message = "Maximum pool size must be at least 1")
        @Max(value = 100, message = "Maximum pool size must be at most 100")
        @DefaultValue("10")
        int maximumPoolSize
    ) {
    }
    public record ShardGroup(
        @NotNull
        @NotBlank
        String name,
        @Min(value = 1, message = "Shard count must be at least 1")
        @Max(value = ShardedId.SHARD_CAPACITY, message = "Shard count must be at most " + ShardedId.SHARD_CAPACITY)
        int shardCount,
        List<@Valid ShardRoute> routes
    ) {
        public void validate() {
            if (!isPowerOfTwo(this.shardCount())) {
                throw new ShardingConfigurationException("Shard count must be a power of two");
            }
            if (ShardedId.SHARD_CAPACITY % this.shardCount() != 0) {
                throw new ShardingConfigurationException(
                        "Shard count must evenly divide " + ShardedId.SHARD_CAPACITY);
            }
        }
        public static boolean isPowerOfTwo(int n) {
            return n > 0 && (n & (n - 1)) == 0;
        }

        /**
         * 每个物理分片(表)对应多少个逻辑 shardId(SHARD_CAPACITY / shardCount)。
         * 由于两者都保证为 2 的幂，这里永远整除。
         */
        public int shardsPerPhysical() {
            return ShardedId.SHARD_CAPACITY / this.shardCount();
        }

        /**
         * 将 IdLayout 中编码的逻辑 shardId 折算为当前物理分片索引 [0, shardCount).
         */
        public int toPhysicalShardId(long logicalShardId) {
            return (int) (logicalShardId / shardsPerPhysical());
        }
    }

    public record ShardRoute(
        int from,
        int to,
        @NotNull
        @NotBlank
        String instance,
        @NotNull
        @NotBlank
        String database
    ){}

}
