package com.ovelin.mall.auth.application.port;

import com.ovelin.mall.auth.domain.model.enums.IdentityType;

public interface VerificationCodeVerifier {
    /** 校验发给 (type, identifier) 的验证码，通过返回 true */
    boolean verify(IdentityType type, String identifier, String code);
}