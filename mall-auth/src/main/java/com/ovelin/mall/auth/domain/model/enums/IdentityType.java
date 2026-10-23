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

    public void requireSupportsMfa() {
        if (this == PHONE || this == EMAIL){
            return;
        }
        throw new InvalidArgumentException(this + "不支持开启二次校验");
    }
}