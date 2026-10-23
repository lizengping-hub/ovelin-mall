package com.ovelin.mall.auth.application.service;

import com.ovelin.mall.auth.application.assembler.IdentifierAssembler;
import com.ovelin.mall.auth.application.command.RegisterCommand;
import com.ovelin.mall.auth.application.port.AuthIdFactory;
import com.ovelin.mall.auth.application.port.VerificationCodeVerifier;
import com.ovelin.mall.auth.domain.exception.VerificationFailedException;
import com.ovelin.mall.auth.domain.model.aggregate.LoginAccount;
import com.ovelin.mall.auth.domain.model.aggregate.UserPassword;
import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifier;
import com.ovelin.mall.auth.domain.model.identityIdentifier.PhoneIdentifier;
import com.ovelin.mall.auth.domain.repository.LoginAccountRepository;
import com.ovelin.mall.auth.domain.repository.UserPasswordRepository;
import com.ovelin.mall.common.exception.global.InvalidArgumentException;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.user.api.UserApi;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RegisterAppService {
    private final AuthIdFactory authIdFactory;
    private final PasswordEncoder passwordEncoder;
    private final LoginAccountRepository loginAccountRepository;
    private final UserPasswordRepository userPasswordRepository;
    private final UserApi userApi;
    private final VerificationCodeVerifier verificationCodeVerifier;
    private final TransactionTemplate transactionTemplate;

    public void register(RegisterCommand command) {
        IdentityType identityType = IdentityType.fromCode(command.type());

        IdentityIdentifier identifier = IdentifierAssembler.toIdentityIdentifier(identityType, command.identifier());

        // 外部调用，放在事务外
        verifyCodeIfNeeded(identityType, identifier, command.verifyCode());

        ShardedId userId = authIdFactory.newUserId(identifier);
        ShardedId identityId = authIdFactory.newIdentityId(userId);
        LoginAccount account = LoginAccount.open(userId, identityId, identifier);

        UserPassword userPassword = createUserPasswordIfNeeded(userId, identityType, command.password());

        if (command.addMfaFactor()) {
            account.addIdentityFactor(authIdFactory.newFactorId(userId), identityId);
        }

        String displayName = command.displayName() != null
                ? command.displayName()
                : resolveDisplayName(identifier, userId);

        // 落库：account、password、user 同一事务，任何 RuntimeException 都会回滚
        transactionTemplate.executeWithoutResult(status ->
                persist(account, userPassword, displayName, command.registerSource()));
    }

    private void persist(LoginAccount account, UserPassword userPassword,
                         String displayName, String registerSource) {
        loginAccountRepository.save(account);
        if (userPassword != null) {
            userPasswordRepository.add(userPassword);
        }
        userApi.createUser(account.getUserId().value(), displayName, registerSource);
    }


    private UserPassword createUserPasswordIfNeeded(ShardedId userId, IdentityType identityType, String password){
        if (identityType.needPassword()){
            if (password == null || password.isBlank()){
                throw new InvalidArgumentException("密码不能为空");
            }
            return UserPassword.create(userId, passwordEncoder.encode(password), Instant.now());
        }
        return null;
    }
    private void verifyCodeIfNeeded(IdentityType type, IdentityIdentifier identifier, String code) {
        if (!type.needVerifyCode()) {
            return;
        }
        if (code == null || code.isBlank() || !verificationCodeVerifier.verify(identifier, code)) {
            throw new VerificationFailedException();
        }
    }


    private String resolveDisplayName(IdentityIdentifier identifier, ShardedId userId){
        return switch (identifier.type()){
            case USERNAME -> "user_name_"+userId.sequence();
            case PHONE -> "user_"+((PhoneIdentifier)identifier).rawNumber();
            case EMAIL -> identifier.value().split("@")[0];
            case OAUTH -> "user_"+userId.sequence();
        };
    }

}
