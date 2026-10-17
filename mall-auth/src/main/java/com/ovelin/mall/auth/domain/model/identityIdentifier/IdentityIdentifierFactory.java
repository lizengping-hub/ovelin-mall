package com.ovelin.mall.auth.domain.model.identityIdentifier;

import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import com.ovelin.mall.common.exception.global.InvalidArgumentException;

public final class IdentityIdentifierFactory {

    private IdentityIdentifierFactory() {}

    public static IdentityIdentifier reconstitute(
            String type, String value) {
        IdentityType identityType = IdentityType.fromCode(type);
        return new SimpleIdentifier(identityType, value);
    }
}