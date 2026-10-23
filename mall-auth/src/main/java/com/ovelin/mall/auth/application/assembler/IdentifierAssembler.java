package com.ovelin.mall.auth.application.assembler;

import com.ovelin.mall.auth.application.command.RegisterCommand;
import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import com.ovelin.mall.auth.domain.model.identityIdentifier.EmailIdentifier;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifier;
import com.ovelin.mall.auth.domain.model.identityIdentifier.PhoneIdentifier;
import com.ovelin.mall.auth.domain.model.identityIdentifier.UserNameIdentifier;
import com.ovelin.mall.common.exception.global.InvalidArgumentException;


public class IdentifierAssembler {
    public static IdentityIdentifier toIdentityIdentifier(IdentityType type, String identifier){
        return switch (type){
            case USERNAME -> UserNameIdentifier.from(identifier);
            case PHONE -> PhoneIdentifier.from(identifier);
            case EMAIL -> EmailIdentifier.from(identifier);
            case OAUTH -> throw new InvalidArgumentException("Oauth 暂不支持");
        };

    }
}
