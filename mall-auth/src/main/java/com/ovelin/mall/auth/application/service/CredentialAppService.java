package com.ovelin.mall.auth.application.service;

import com.ovelin.mall.auth.domain.exception.AccountNotFoundException;
import com.ovelin.mall.auth.domain.exception.PasswordFailedException;
import com.ovelin.mall.auth.domain.model.aggregate.UserPassword;
import com.ovelin.mall.auth.domain.repository.UserPasswordRepository;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class CredentialAppService {

    private final UserPasswordRepository passwordRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public void changePassword(ShardedId userId, String oldPassword, String newPassword) {
        UserPassword password = passwordRepository.findByUserId(userId)
                .orElseThrow(()->new AccountNotFoundException(userId));

//        if (!passwordEncoder.matches(oldPassword, password.getEncodedPassword())) {
//            throw new PasswordFailedException();
//        }
//        password.change(passwordEncoder.encode(newPassword), clock.instant());
        passwordRepository.update(password);
    }
}