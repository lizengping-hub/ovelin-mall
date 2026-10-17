package com.ovelin.mall.auth.domain.model.identityIdentifier;

import com.ovelin.mall.auth.domain.model.enums.IdentityType;

class SimpleIdentifier extends IdentityIdentifier {
    private final String value;
    private final IdentityType identityType;

    SimpleIdentifier(IdentityType identityType, String value) {
        this.value = value;
        this.identityType = identityType;
    }

    @Override
    public IdentityType type() {
        return identityType;
    }

    @Override
    public String value() {
        return value;
    }
}
