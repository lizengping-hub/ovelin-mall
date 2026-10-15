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

    /** 标准化登录标识：去首尾空白，邮箱和用户名统一小写。手机号的国家码规范化应在入口处理好再传进来。 */
    public String normalize(String raw) {
        String value = raw.trim();
        return this == PHONE ? value : value.toLowerCase(Locale.ROOT);
    }

    public boolean canReceiveCode() {
        return this == PHONE || this == EMAIL;
    }
}