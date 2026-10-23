package com.ovelin.mall.auth.application.service;

import com.ovelin.mall.auth.application.assembler.IdentifierAssembler;
import com.ovelin.mall.auth.application.command.RegisterCommand;
import com.ovelin.mall.auth.application.port.AuthIdFactory;
import com.ovelin.mall.auth.application.port.OvelinPasswordEncoder;
import com.ovelin.mall.auth.application.port.VerificationCodeVerifier;
import com.ovelin.mall.auth.domain.exception.VerificationFailedException;
import com.ovelin.mall.auth.domain.model.aggregate.LoginAccount;
import com.ovelin.mall.auth.domain.model.aggregate.UserPassword;
import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifier;
import com.ovelin.mall.auth.domain.model.identityIdentifier.PhoneIdentifier;
import com.ovelin.mall.auth.domain.model.valueobject.Password;
import com.ovelin.mall.auth.domain.repository.LoginAccountRepository;
import com.ovelin.mall.auth.domain.repository.UserPasswordRepository;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.user.api.UserApi;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RegisterAppService {
    private final AuthIdFactory authIdFactory;
    private final OvelinPasswordEncoder passwordEncoder;
    private final LoginAccountRepository loginAccountRepository;
    private final UserPasswordRepository userPasswordRepository;
    private final UserApi userApi;
    private final VerificationCodeVerifier verificationCodeVerifier;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public void register(RegisterCommand command) {
        IdentityType identityType = IdentityType.fromCode(command.type());

        if (command.addMfaFactor()){
            // fail-fast：不消耗验证码、不消耗 id，规则以 forIdentity 为准，这里仅为提前失败，最终实现都在 identityType.requireSupportsMfa();
            identityType.requireSupportsMfa();
        }
        IdentityIdentifier identifier = IdentifierAssembler.toIdentityIdentifier(identityType, command.identifier());
        Password password = createPasswordIfNeeded(command, identityType);

        // 外部调用，放在事务外
        verifyCodeIfNeeded(identityType, identifier, command.verifyCode());


        ShardedId userId = authIdFactory.newUserId(identifier);
        ShardedId identityId = authIdFactory.newIdentityId(userId);
        LoginAccount account = LoginAccount.open(userId, identityId, identifier);

        if (command.addMfaFactor()) {
            account.addIdentityFactor(authIdFactory.newFactorId(userId), identityId, clock.instant());
        }
        UserPassword userPassword = createUserPasswordIfNeeded(userId, password);


        String displayName = resolveDisplayName(command, identifier, userId);

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

    private static @Nullable Password createPasswordIfNeeded(RegisterCommand command, IdentityType identityType) {
        if (identityType.needPassword()){
            return Password.from(command.password());
        }
        return null;
    }

    private UserPassword createUserPasswordIfNeeded(ShardedId userId, Password password){
        if (password != null){
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


    private String resolveDisplayName(RegisterCommand command, IdentityIdentifier identifier, ShardedId userId) {
        return command.displayName() != null ?
                command.displayName()
                : switch (identifier.type()) {
                    case USERNAME -> "user_name_" + userId.sequence();
                    case PHONE -> "user_" + ((PhoneIdentifier) identifier).rawNumber();
                    case EMAIL -> identifier.value().split("@")[0];
                    case OAUTH -> "user_" + userId.sequence();
        };
    }

}
