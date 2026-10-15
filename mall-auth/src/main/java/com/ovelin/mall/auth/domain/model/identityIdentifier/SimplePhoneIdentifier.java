package com.ovelin.mall.auth.domain.model.identityIdentifier;

import com.ovelin.mall.auth.domain.model.enums.IdentityType;

class SimplePhoneIdentifier extends IdentityIdentifier {
    private final String phoneNumberE164;

    private SimplePhoneIdentifier(String phoneNumberE164) {
        this.phoneNumberE164 = phoneNumberE164;
    }

    @Override
    public IdentityType type() {
        return IdentityType.PHONE;
    }

    @Override
    public String value() {
        return phoneNumberE164;
    }
    static SimplePhoneIdentifier reconstitute(String value){
        return new SimplePhoneIdentifier(value);
    }
}
