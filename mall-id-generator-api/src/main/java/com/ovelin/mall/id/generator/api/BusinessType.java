package com.ovelin.mall.id.generator.api;

import java.util.Objects;

public record BusinessType(String value) {
    public BusinessType{
        Objects.requireNonNull(value);
    }
}
