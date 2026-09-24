package com.ovelin.mall.id.generator.starter.application.dto;

import java.util.Objects;

public record BusinessType(String value) {
    public BusinessType{
        Objects.requireNonNull(value);
    }
}
