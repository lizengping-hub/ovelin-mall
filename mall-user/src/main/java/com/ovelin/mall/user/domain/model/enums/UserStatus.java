package com.ovelin.mall.user.domain.model.enums;


import com.ovelin.mall.user.domain.exception.InvalidArgumentException;
import lombok.Getter;

import java.util.Objects;

@Getter
public enum UserStatus {
    DISABLED(0),
    ACTIVE(1),
    DELETED(2);

    private final int value;

    UserStatus(int value) { this.value = value; }

    public static UserStatus fromValue(Integer value) {
        Objects.requireNonNull(value,"status must not be null");
        for (UserStatus s : values()) {
            if (s.value == value) return s;
        }
        throw new InvalidArgumentException("unknown user status: " + value);
    }
}