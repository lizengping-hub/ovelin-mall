package com.ovelin.mall.auth.domain.model.enums;

import com.ovelin.mall.common.exception.global.InvalidArgumentException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum IdentityType {
    USERNAME("username"),
    PHONE("phone"),
    EMAIL("email"),
    OAUTH("oauth");

    private final String code;

    public static IdentityType fromCode(String code) {
        for (IdentityType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new InvalidArgumentException("未知的身份类型: " + code);
    }

    public boolean canReceiveCode() {
        return this == PHONE || this == EMAIL;
    }
}