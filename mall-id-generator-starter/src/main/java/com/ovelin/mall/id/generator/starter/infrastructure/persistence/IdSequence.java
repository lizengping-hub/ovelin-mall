package com.ovelin.mall.id.generator.starter.infrastructure.persistence;

public record IdSequence(
        String sequenceName,
        long nextValue,
        int allocationSize,
        long version) {
}
