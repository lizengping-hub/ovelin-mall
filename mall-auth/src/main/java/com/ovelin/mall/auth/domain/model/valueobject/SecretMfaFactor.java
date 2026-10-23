package com.ovelin.mall.auth.domain.model.valueobject;

import com.ovelin.mall.auth.domain.model.enums.MfaFactorType;
import com.ovelin.mall.common.exception.global.InvalidArgumentException;

public record SecretMfaFactor(MfaFactorType type, String secretEncrypted) {
    public SecretMfaFactor {
        if (!type.isSecretBased()) throw new InvalidArgumentException("不是密码类型的二次验证");
        if (secretEncrypted == null || secretEncrypted.isBlank()) throw new InvalidArgumentException("密码加密不能为空");
    }
}