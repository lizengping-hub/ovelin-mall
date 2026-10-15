package com.ovelin.mall.auth.domain.model.identityIdentifier;

import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import com.ovelin.mall.auth.domain.model.valueobject.Email;


public class EmailIdentifier extends IdentityIdentifier{
    private final Email email;

    private EmailIdentifier(Email email){
        this.email = email;
    }
    @Override
    public IdentityType type() {
        return IdentityType.EMAIL;
    }

    @Override
    public String value() {
        return email.getValue();
    }

    public static IdentityIdentifier from(Email email) {
        return new EmailIdentifier(email);
    }

    public static IdentityIdentifier from(String identifier) {
        return new EmailIdentifier(Email.from(identifier));
    }

    public static EmailIdentifier reconstitute(String identifier) {
        return new EmailIdentifier(Email.reconstitute(identifier));
    }



}
