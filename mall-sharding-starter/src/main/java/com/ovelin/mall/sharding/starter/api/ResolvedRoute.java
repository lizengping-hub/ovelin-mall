package com.ovelin.mall.sharding.starter.api;

public record ResolvedRoute(
        int shardId,
        String instanceKey,
        String databaseName) {

    public String resolveTableName(String tableName) {
        if (shardId < 0) {
            return String.format("%s.%s", databaseName, tableName);
        }
        return String.format("%s.%s_%s", databaseName, tableName, suffix(shardId));
    }
    private static String suffix(int shardId) {
        if (shardId < 0 || shardId > 9999) {
            throw new IllegalArgumentException("Shard ID cannot be represented as a table suffix: " + shardId);
        }
        return "%04d".formatted(shardId);
    }
}