package com.ovelin.mall.auth.domain.model.valueobject;

import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import com.ovelin.mall.auth.domain.model.enums.MfaFactorType;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifier;
import com.ovelin.mall.common.exception.global.InvalidArgumentException;

public record IdentityMfaFactor(IdentityIdentifier identifier) {
    public IdentityMfaFactor {
        if (!identifier.type().supportsMfa()) {
            throw new InvalidArgumentException(identifier.type() + " 不支持二次验证");
        }
    }
    public MfaFactorType type() {
        if (identifier.type() == IdentityType.PHONE){
            return MfaFactorType.TOTP;
        }
        if ( identifier.type() == IdentityType.EMAIL){
            return MfaFactorType.EMAIL;
        }
        throw new IllegalArgumentException("IdentityType 到 MfaFactorType 的枚举映射不足");
    }
}

