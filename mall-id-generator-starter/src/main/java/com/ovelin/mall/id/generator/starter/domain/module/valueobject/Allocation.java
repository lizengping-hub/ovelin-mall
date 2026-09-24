package com.ovelin.mall.id.generator.starter.domain.module.valueobject;

import java.util.Arrays;

public record Allocation(
        long start,
        int count
) {
    public boolean hasOne() {
        return count == 1;
    }
}