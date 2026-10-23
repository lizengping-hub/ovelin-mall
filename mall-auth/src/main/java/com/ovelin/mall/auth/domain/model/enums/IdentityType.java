package com.ovelin.mall.auth.domain.model.enums;

import com.ovelin.mall.common.exception.global.InvalidArgumentException;
import lombok.Getter;

import java.util.Objects;

@Getter
public enum IdentityType {
    USERNAME("username"),
    PHONE("phone"),
    EMAIL("email"),
    OAUTH("oauth");

    private final String code;

    IdentityType(String code){
        this.code = code;
    }

    public static IdentityType fromCode(String code) {
        if (Objects.isNull(code) || code.trim().isEmpty()){
            throw new InvalidArgumentException("身份类型不可为空: " + code);
        }
        for (IdentityType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new InvalidArgumentException("未知的身份类型: " + code);
    }

    public boolean needPassword(){
        return this == USERNAME;
    }

    public boolean needVerifyCode() {
        return this == PHONE || this == EMAIL;
    }

    /** 能否在注册时绑定二次验证因子（需要有可用通道） */
    public boolean supportsMfa() {
        return this == PHONE || this == EMAIL;
    }
}