package com.ovelin.mall.auth.domain.model.enums;

import com.ovelin.mall.common.exception.global.InvalidArgumentException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MfaFactorType {
    SMS("sms"),
    EMAIL("email"),
    TOTP("totp"),
    WEBAUTHN("webauthn"),
    RECOVERY_CODE("recovery_code");

    private final String code;

    public static MfaFactorType fromCode(String code) {
        for (MfaFactorType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new InvalidArgumentException("未知的二次验证类型: " + code);
    }

    /** 短信、邮箱因子引用 user_identity，不自己存密钥 */
    public boolean referencesIdentity() {
        return this == SMS || this == EMAIL;
    }
}