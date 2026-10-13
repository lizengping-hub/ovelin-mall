package com.ovelin.mall.common.sharding.core.core;

import com.ovelin.mall.common.sharding.core.api.ShardResolver;
import com.ovelin.mall.common.sharding.core.ov.ShardId;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class HashShardResolver implements ShardResolver {
    @Override
    public String buildShardKey(String shardKey0, String shardKey1){
        Objects.requireNonNull(shardKey0, "shardKey0 cannot be null");
        Objects.requireNonNull(shardKey1, "shardKey1 cannot be null");
        return shardKey0+":"+shardKey1;
    }
    @Override
    public ShardId resolve(String shardKey0, String shardKey1){
        return resolve(buildShardKey(shardKey0,shardKey1));
    }
    @Override
    public ShardId resolve(String shardKey) {
        Objects.requireNonNull(shardKey, "shardKey cannot be null");
        byte[] hash = sha256(shardKey);
        long unsignedPrefix = ((long) (hash[0] & 0xff) << 24)
                | ((long) (hash[1] & 0xff) << 16)
                | ((long) (hash[2] & 0xff) << 8)
                | (hash[3] & 0xffL);
        return ShardId.of((int)(unsignedPrefix % ShardId.SHARD_CAPACITY));
    }

    private static byte[] sha256(String shardKey) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(shardKey.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
