package com.ovelin.mall.auth.infrastructure.persistence.redis;

import com.ovelin.mall.auth.application.port.VerificationCodeVerifier;
import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import org.springframework.stereotype.Component;

@Component
public class VerificationCodeVerifierImpl implements VerificationCodeVerifier {
    @Override
    public boolean verify(IdentityType type, String identifier, String code) {
        return true;
    }
}
