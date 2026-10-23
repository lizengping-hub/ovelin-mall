package com.ovelin.mall.auth.application.service;

import com.ovelin.mall.auth.application.command.RegisterCommand;
import com.ovelin.mall.auth.application.port.AuthIdFactory;
import com.ovelin.mall.auth.application.port.VerificationCodeVerifier;
import com.ovelin.mall.auth.domain.exception.VerificationFailedException;
import com.ovelin.mall.auth.domain.model.aggregate.LoginAccount;
import com.ovelin.mall.auth.domain.model.aggregate.UserPassword;
import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import com.ovelin.mall.auth.domain.repository.LoginAccountRepository;
import com.ovelin.mall.auth.domain.repository.UserPasswordRepository;
import com.ovelin.mall.common.exception.global.InvalidArgumentException;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.user.api.UserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterAppServiceTest {

    @Mock
    AuthIdFactory authIdFactory;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    LoginAccountRepository loginAccountRepository;
    @Mock
    UserPasswordRepository userPasswordRepository;
    @Mock
    UserApi userApi;
    @Mock
    VerificationCodeVerifier verificationCodeVerifier;
    @Spy
    TransactionTemplate transactionTemplate =
            new TransactionTemplate(Mockito.mock(PlatformTransactionManager.class));
    @InjectMocks
    RegisterAppService service;

    private ShardedId userId;
    private ShardedId identityId;
    private ShardedId factorId;

    @BeforeEach
    void setUp() {
        // TODO: 换成你们测试里用 IdGenerator / ShardedId 构造的方式
        userId = ShardedId.fromId(1001L);
        identityId = ShardedId.fromId(1002L);
        factorId = ShardedId.fromId(1003L);
        lenient().when(verificationCodeVerifier.verify(any(), anyString())).thenReturn(true);
        lenient().when(authIdFactory.newUserId(any())).thenReturn(userId);
        lenient().when(authIdFactory.newIdentityId(userId)).thenReturn(identityId);
        lenient().when(authIdFactory.newFactorId(userId)).thenReturn(factorId);
        lenient().when(passwordEncoder.encode(anyString())).thenAnswer(i -> "enc:" + i.getArgument(0));
    }

    // ---------- 正常流程 ----------

    @Nested
    class Success {

        @Test
        void username_with_password_saves_account_password_and_creates_user() {
            Instant before = Instant.now();

            service.register(cmd("username", "alice", "Passw0rd!", null, "WEB", false, null));

            verify(passwordEncoder).encode("Passw0rd!");

            ArgumentCaptor<UserPassword> pwd = ArgumentCaptor.forClass(UserPassword.class);
            verify(userPasswordRepository).add(pwd.capture());
            assertThat(pwd.getValue().getPasswordChangedAt()).isBetween(before, Instant.now());

            verify(loginAccountRepository).save(any(LoginAccount.class));
            verify(userApi).createUser(1001L, "user_name_" + userId.sequence(), "WEB");
        }

        @Test
        void saves_before_creating_user() {
            service.register(cmd("username", "alice", "Passw0rd!", null, "WEB", false, null));

            InOrder order = inOrder(loginAccountRepository, userPasswordRepository, userApi);
            order.verify(loginAccountRepository).save(any());
            order.verify(userPasswordRepository).add(any());
            order.verify(userApi).createUser(anyLong(), anyString(), anyString());
        }

        @Test
        void identity_without_password_does_not_touch_password_repository() {
            // 前提：EMAIL 不需要密码（needPassword()==false），按实际枚举调整

            service.register(cmd("email", "bob@example.com", null, null, "WEB", false, "12345"));

            verifyNoInteractions(passwordEncoder, userPasswordRepository);
            verify(loginAccountRepository).save(any());
        }
    }

    // ---------- displayName ----------

    @Nested
    class DisplayName {

        @Test
        void explicit_display_name_is_used_as_is() {
            service.register(cmd("username", "alice", "pw", "Alice", "WEB", false, null));

            verify(userApi).createUser(1001L, "Alice", "WEB");
        }

        @Test
        void email_uses_local_part() {
            service.register(cmd("email", "bob@example.com", null, null, "WEB", false, "12345"));

            verify(userApi).createUser(1001L, "bob", "WEB");
        }

        @Test
        void phone_uses_raw_value() {
            service.register(cmd("phone", "13800138000", null, null, "SMS", false, "12345"));

            verify(userApi).createUser(1001L, "user_13800138000", "SMS");
        }
    }

    // ---------- Verification code ----------

    @Nested
    class VerificationCode {

        @Test
        void no_password_and_no_verify_code_throw_verification_failed_exception() {
            assertThatThrownBy(()->
                    service.register(cmd("email", "bob@example.com", null, null, "WEB", false, null))
                    ).isInstanceOf(VerificationFailedException.class);
            verifyNoInteractions(loginAccountRepository, userPasswordRepository);
        }

        @Test
        void username_does_not_use_verification_code() {
            service.register(cmd("username", "alice", "pw", null, "WEB", false, null));

            assertThat(captureSavedAccount().getIdentities()).hasSize(1);
            verifyNoInteractions(verificationCodeVerifier);   // 用户名不走验证码
        }

        @Test
        void phone_with_valid_code_is_registered() {
            when(verificationCodeVerifier.verify(any(), eq("123456"))).thenReturn(true);

            service.register(cmd("phone", "13800138000", null, null, "SMS", false, "123456"));

            assertThat(captureSavedAccount().getIdentities()).hasSize(1);
            verify(verificationCodeVerifier).verify(
                    argThat(id -> id.type() == IdentityType.PHONE && id.value().equals("+8613800138000")),
                    eq("123456"));
        }
    }

    // ---------- MFA ----------
    @Nested
    class Mfa{
        @Test
        void phone_with_mfa_adds_factor() {
            when(verificationCodeVerifier.verify(any(), anyString())).thenReturn(true);

            service.register(cmd("phone", "13800138000", null, null, "SMS", true, "123456"));

            verify(authIdFactory).newFactorId(userId);
            assertThat(captureSavedAccount().getFactors()).hasSize(1);
        }

        @Test
        void username_with_mfa_is_rejected_before_anything_happens() {
            assertThatThrownBy(() -> service.register(cmd("username", "alice", "pw", null, "WEB", true, null)))
                    .isInstanceOf(InvalidArgumentException.class);

            verifyNoInteractions(authIdFactory, passwordEncoder, verificationCodeVerifier,
                    loginAccountRepository, userPasswordRepository, userApi);
        }
    }

    // ---------- 异常路径 ----------

    @Nested
    class Failure {

        @Test
        void password_required_but_null_throws_and_persists_nothing() {
            assertThatThrownBy(() -> service.register(cmd("username", "alice", null, null, "WEB", false, null)))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasMessage("密码不能为空");

            verifyNoInteractions(loginAccountRepository, userPasswordRepository, userApi);
        }

        @Test
        void unknown_identity_type_fails_before_any_id_is_generated() {
            assertThatThrownBy(() -> service.register(cmd("wechat", "x", null, null, "WEB", false, null)))
                    .isInstanceOf(RuntimeException.class); // 换成 IdentityType.fromCode 实际抛的异常

            verifyNoInteractions(authIdFactory, loginAccountRepository, userApi);
        }

        @Test
        void account_save_failure_stops_password_and_user_creation() {
            doThrow(new IllegalStateException("duplicate identifier"))
                    .when(loginAccountRepository).save(any());

            assertThatThrownBy(() -> service.register(cmd("username", "alice", "pw", null, "WEB", false, null)))
                    .isInstanceOf(IllegalStateException.class);

            verifyNoInteractions(userPasswordRepository, userApi);
        }

        @Test
        void create_user_failure_propagates() {
            doThrow(new IllegalStateException("user api down"))
                    .when(userApi).createUser(anyLong(), anyString(), anyString());

            assertThatThrownBy(() -> service.register(cmd("username", "alice", "pw", null, "WEB", false, null)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("user api down");
        }
        // Failure 里新增
        @Test
        void phone_without_code_is_rejected() {
            assertThatThrownBy(() -> service.register(cmd("phone", "13800138000", null, null, "SMS", false, null)))
                    .isInstanceOf(VerificationFailedException.class);
            verify(transactionTemplate, never()).executeWithoutResult(any());
            verifyNoInteractions(verificationCodeVerifier, authIdFactory, loginAccountRepository, userApi);
        }
        @Test
        void wrong_code_is_rejected_and_persists_nothing() {
            when(verificationCodeVerifier.verify(any(), anyString())).thenReturn(false);

            assertThatThrownBy(() -> service.register(cmd("email", "bob@example.com", null, null, "WEB", false, "000000")))
                    .isInstanceOf(VerificationFailedException.class);
            verify(transactionTemplate, never()).executeWithoutResult(any());

            verifyNoInteractions(authIdFactory, loginAccountRepository, userPasswordRepository, userApi);
        }
    }

    // ---------- helpers ----------

    private RegisterCommand cmd(String type, String identifier, String password, String displayName,
                                String source, boolean mfa, String verifyCode) {
        return new RegisterCommand(type, identifier, password, displayName, source, mfa, verifyCode);
    }

    private LoginAccount captureSavedAccount() {
        ArgumentCaptor<LoginAccount> captor = ArgumentCaptor.forClass(LoginAccount.class);
        verify(loginAccountRepository).save(captor.capture());
        return captor.getValue();
    }

}