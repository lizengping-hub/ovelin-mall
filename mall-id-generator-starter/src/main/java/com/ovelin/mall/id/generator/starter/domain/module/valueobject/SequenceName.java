package com.ovelin.mall.id.generator.starter.domain.module.valueobject;

import jakarta.validation.constraints.NotNull;

public record SequenceName(@NotNull String value) {
    public SequenceName {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Sequence name cannot be null or empty");
        }
    }
    public static SequenceName of(String value) {
        return new SequenceName(value);
    }
    @Override
    @NotNull
    public String toString() {
        return value;
    }
}
