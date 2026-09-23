package com.ovelin.mall.id.generator.starter.domain.module.valueobject;

public record AllocationSize(int value) {
    public AllocationSize {
        if (value <= 0) {
            throw new IllegalArgumentException("Allocation size must be positive");
        }
    }
}
