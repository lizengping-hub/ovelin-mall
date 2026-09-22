package com.ovelin.mall.sharding.starter.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ShardCalculator {
    public int shardIdFor(Object shardKey) {
        byte[] hash = sha256(shardKey.toString());
        long unsignedPrefix = ((long) (hash[0] & 0xff) << 24)
                | ((long) (hash[1] & 0xff) << 16)
                | ((long) (hash[2] & 0xff) << 8)
                | (hash[3] & 0xffL);
        return (int) (unsignedPrefix % ShardedId.SHARD_CAPACITY);
    }

    private byte[] sha256(String shardKey) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(shardKey.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
