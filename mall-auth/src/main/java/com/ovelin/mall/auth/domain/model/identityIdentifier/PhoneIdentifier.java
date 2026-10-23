package com.ovelin.mall.auth.domain.model.identityIdentifier;

import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import com.ovelin.mall.auth.domain.model.valueobject.PhoneNumber;


public class PhoneIdentifier extends IdentityIdentifier {
    private final PhoneNumber phoneNumber;

    private PhoneIdentifier(PhoneNumber phoneNumber){
        this.phoneNumber = phoneNumber;
    }
    @Override
    public IdentityType type() {
        return IdentityType.PHONE;
    }

    @Override
    public String value() {
        return phoneNumber.getE164();
    }

    public String rawNumber(){
        return phoneNumber.getRawNumber();
    }

    public static IdentityIdentifier from(String rawInput) {
        return new PhoneIdentifier(PhoneNumber.parseRawInput(rawInput));
    }

    public static IdentityIdentifier from(String rawInput, String defaultRegion) {
        return new PhoneIdentifier(PhoneNumber.parseRawInput(rawInput, defaultRegion));
    }

    public static IdentityIdentifier from(PhoneNumber phoneNumber) {
        return new PhoneIdentifier(phoneNumber);
    }

}
