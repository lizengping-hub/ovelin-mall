package com.ovelin.mall.id.generator.starter.infrastructure.persistence.po;

public record IdSequencePO(
        String sequenceName,
        int shardId,
        long nextValue,
        long version) {
}
