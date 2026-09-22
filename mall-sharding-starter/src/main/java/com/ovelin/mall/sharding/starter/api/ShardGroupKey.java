package com.ovelin.mall.sharding.starter.api;

/**
 * 分片组标识，由使用方在配置文件（ovelin.sharding.shard-routes 下的 key）
 * 和代码中自行定义（如 "user"、"order"、"login"），sharding-starter 本身不预设具体业务域。
 */
public record ShardGroupKey(String value) {
    public ShardGroupKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ShardGroupKey must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
