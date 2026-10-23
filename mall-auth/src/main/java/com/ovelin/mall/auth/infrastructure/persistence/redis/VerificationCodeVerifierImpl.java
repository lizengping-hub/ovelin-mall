package com.ovelin.mall.auth.infrastructure.persistence.redis;

import com.ovelin.mall.auth.application.port.VerificationCodeVerifier;
import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifier;
import org.springframework.stereotype.Component;

@Component
public class VerificationCodeVerifierImpl implements VerificationCodeVerifier {
    @Override
    public boolean verify(IdentityIdentifier identifier, String code) {
        if (code == null){
            return false;
        }
        return true;
    }
}
