package com.ovelin.mall.id.generator.starter.infrastructure.persistence.po;

public record IdSequencePO(
        String sequenceName,
        long nextValue,
        int allocationSize,
        long version) {
}
