package com.ovelin.mall.auth.infrastructure.security;

import com.ovelin.mall.auth.application.port.OvelinPasswordEncoder;
import com.ovelin.mall.auth.domain.model.valueobject.EncodedPassword;
import com.ovelin.mall.auth.domain.model.valueobject.Password;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class OvelinPasswordEncoderImpl implements OvelinPasswordEncoder {
    private final PasswordEncoder passwordEncoder;
    @Override
    public @NonNull EncodedPassword encode(@NonNull Password password) {
        return new EncodedPassword(passwordEncoder.encode(password.getValue()));
    }

    @Override
    public boolean matches(@NonNull Password password, @NonNull EncodedPassword encodedPassword) {
        return passwordEncoder.matches(password.getValue(), encodedPassword.value());
    }

    @Override
    public boolean upgradeEncoding(@NonNull EncodedPassword encodedPassword) {
        return passwordEncoder.upgradeEncoding(encodedPassword.value());
    }
}
