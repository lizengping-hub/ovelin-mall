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

    public static List<ShardKey2Record> getSequenceTwoShardKeys(int shardCount){
        Map<Integer, ShardKey2Record> shardIdMap = new HashMap<>();
        String shardKey0 = "EMAIL";
        for (int i = 0; i < 1000; i++) {
            String shardKey1 = "identity:" + i;
            ShardId shardId = SHARD_RESOLVER.resolve(shardKey0, shardKey1);
            if (shardId.value() % shardCount < shardCount){
                shardIdMap.put(shardId.value() % shardCount, new ShardKey2Record(shardId.value() % shardCount, shardId, shardKey0, shardKey1));
            }
            if (shardIdMap.size() == shardCount){
                break;
            }
        }
        if (shardIdMap.size() < shardCount){
            throw new IllegalStateException("Not enough sharding keys found for the given shard count");
        }
        return List.copyOf(shardIdMap.values()).stream().sorted(Comparator.comparingInt(ShardKey2Record::shardIndex)).toList();
    }

    public record ShardKey2Record(int shardIndex, ShardId shardId, String shardKey0, String shardKey1) {
    }
}
