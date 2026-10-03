package com.ovelin.mall.common.sharding.starter.utils;

import com.ovelin.mall.common.sharding.core.api.ShardResolver;
import com.ovelin.mall.common.sharding.core.core.HashShardResolver;
import com.ovelin.mall.common.sharding.core.ov.ShardId;

import java.util.*;

public class SequenceShardKeyResolver {
    public static final ShardResolver SHARD_RESOLVER = new HashShardResolver();

    /**
     * 根据分片数量获取对应的分片键列表
     * @param shardCount
     * @return
     */
    public static List<ShardKeyRecord> getSequenceShardKeys(int shardCount){
        Map<Integer, ShardKeyRecord> shardIdMap = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            String shardingKey = "identity:" + i;
            ShardId shardId = SHARD_RESOLVER.resolve(shardingKey);
            if (shardId.value() % shardCount < shardCount){
                shardIdMap.put(shardId.value() % shardCount, new ShardKeyRecord(shardId.value() % shardCount, shardId, shardingKey));
            }
            if (shardIdMap.size() == shardCount){
                break;
            }
        }
        if (shardIdMap.size() < shardCount){
            throw new IllegalStateException("Not enough sharding keys found for the given shard count");
        }
        return List.copyOf(shardIdMap.values()).stream().sorted(Comparator.comparingInt(ShardKeyRecord::shardIndex)).toList();
    }

    public record ShardKeyRecord(int shardIndex, ShardId shardId, String shardKey) {
    }
}
