package com.ovelin.mall.auth.application.port;

import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifier;

public interface VerificationCodeVerifier {
    /** 校验发给 (type, identifier) 的验证码，通过返回 true */
    boolean verify(IdentityIdentifier identifier, String code);
}