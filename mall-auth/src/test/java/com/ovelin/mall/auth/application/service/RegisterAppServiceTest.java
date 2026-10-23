package com.ovelin.mall.auth.application.service;

import com.ovelin.mall.auth.application.command.RegisterCommand;
import com.ovelin.mall.auth.application.port.AuthIdFactory;
import com.ovelin.mall.auth.application.port.OvelinPasswordEncoder;
import com.ovelin.mall.auth.application.port.VerificationCodeVerifier;
import com.ovelin.mall.auth.domain.exception.VerificationFailedException;
import com.ovelin.mall.auth.domain.model.aggregate.LoginAccount;
import com.ovelin.mall.auth.domain.model.aggregate.UserPassword;
import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import com.ovelin.mall.auth.domain.model.enums.MfaFactorStatus;
import com.ovelin.mall.auth.domain.model.enums.MfaFactorType;
import com.ovelin.mall.auth.domain.model.valueobject.EncodedPassword;
import com.ovelin.mall.auth.domain.model.valueobject.Password;
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

    /** 用一个肯定通过 Password 校验的值，避免用例因密码强度规则变化而失败 */
    private static final String PASSWORD = "Passw0rd!";

    @Mock
    AuthIdFactory authIdFactory;
    @Mock
    OvelinPasswordEncoder passwordEncoder;
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
        // 前提：encode(Password) 返回 String（哈希）；如果返回的是值对象，这里改成对应类型
        lenient().when(passwordEncoder.encode(any(Password.class))).thenReturn(new EncodedPassword("enc:hash"));
    }

    // ---------- 正常流程 ----------

    @Nested
    class Success {

        @Test
        void username_with_password_saves_account_password_and_creates_user() {
            Instant before = Instant.now();

            service.register(cmd("username", "alice", PASSWORD, null, "WEB", false, null));

            verify(passwordEncoder).encode(any(Password.class));

            ArgumentCaptor<UserPassword> pwd = ArgumentCaptor.forClass(UserPassword.class);
            verify(userPasswordRepository).add(pwd.capture());
            assertThat(pwd.getValue().getPasswordChangedAt()).isBetween(before, Instant.now());

            verify(loginAccountRepository).save(any(LoginAccount.class));
            verify(userApi).createUser(1001L, "user_name_" + userId.sequence(), "WEB");
        }

        @Test
        void saves_before_creating_user() {
            service.register(cmd("username", "alice", PASSWORD, null, "WEB", false, null));

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
            service.register(cmd("username", "alice", PASSWORD, "Alice", "WEB", false, null));

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
        void username_does_not_use_verification_code() {
            service.register(cmd("username", "alice", PASSWORD, null, "WEB", false, null));

            assertThat(captureSavedAccount().getIdentities()).hasSize(1);
            verifyNoInteractions(verificationCodeVerifier);   // 用户名不走验证码
        }

        @Test
        void phone_with_valid_code_is_registered() {
            service.register(cmd("phone", "13800138000", null, null, "SMS", false, "123456"));

            assertThat(captureSavedAccount().getIdentities()).hasSize(1);
            verify(verificationCodeVerifier).verify(
                    argThat(id -> id.type() == IdentityType.PHONE && id.value().equals("+8613800138000")),
                    eq("123456"));
        }

        /** id 是有成本的资源：验证码校验必须先于任何 id 的分配 */
        @Test
        void ids_are_generated_only_after_code_is_verified() {
            service.register(cmd("email", "bob@example.com", null, null, "WEB", false, "12345"));

            InOrder order = inOrder(verificationCodeVerifier, authIdFactory);
            order.verify(verificationCodeVerifier).verify(any(), eq("12345"));
            order.verify(authIdFactory).newUserId(any());
        }
    }

    // ---------- MFA ----------

    @Nested
    class Mfa {

        @Test
        void phone_with_mfa_adds_enabled_sms_factor() {
            service.register(cmd("phone", "13800138000", null, null, "SMS", true, "123456"));

            verify(authIdFactory).newFactorId(userId);
            assertThat(captureSavedAccount().getFactors()).singleElement().satisfies(factor -> {
                assertThat(factor.getType()).isEqualTo(MfaFactorType.SMS);
                assertThat(factor.getIdentityId()).isEqualTo(identityId);
                assertThat(factor.getStatus()).isEqualTo(MfaFactorStatus.ENABLED);   // 创建即启用
            });
        }

        @Test
        void email_with_mfa_adds_enabled_email_factor() {
            service.register(cmd("email", "bob@example.com", null, null, "WEB", true, "12345"));

            assertThat(captureSavedAccount().getFactors()).singleElement().satisfies(factor -> {
                assertThat(factor.getType()).isEqualTo(MfaFactorType.EMAIL);
                assertThat(factor.getStatus()).isEqualTo(MfaFactorStatus.ENABLED);
            });
        }

        @Test
        void without_mfa_no_factor_is_added() {
            service.register(cmd("phone", "13800138000", null, null, "SMS", false, "123456"));

            verify(authIdFactory, never()).newFactorId(any());
            assertThat(captureSavedAccount().getFactors()).isEmpty();
        }

        /** fail-fast：IdentityType.requireSupportsMfa() 在验证码、密码处理、id 分配之前触发 */
        @Test
        void username_with_mfa_is_rejected_before_anything_happens() {
            assertThatThrownBy(() -> service.register(cmd("username", "alice", PASSWORD, null, "WEB", true, null)))
                    .isInstanceOf(InvalidArgumentException.class);

            verifyNoInteractions(authIdFactory, passwordEncoder, verificationCodeVerifier,
                    loginAccountRepository, userPasswordRepository, userApi);
        }
    }

    // ---------- 异常路径 ----------

    @Nested
    class Failure {

        /** Password.from 在验证码和 id 之前执行，失败时不能消耗它们，也不会走到加密 */
        @Test
        void password_required_but_null_fails_fast_before_code_ids_and_encoding() {
            assertThatThrownBy(() -> service.register(cmd("username", "alice", null, null, "WEB", false, null)))
                    .isInstanceOf(InvalidArgumentException.class);

            verifyNoInteractions(authIdFactory, passwordEncoder, verificationCodeVerifier,
                    loginAccountRepository, userPasswordRepository, userApi);
        }

        @Test
        void blank_password_fails_fast_before_code_ids_and_encoding() {
            assertThatThrownBy(() -> service.register(cmd("username", "alice", "   ", null, "WEB", false, null)))
                    .isInstanceOf(InvalidArgumentException.class);

            verifyNoInteractions(authIdFactory, passwordEncoder, verificationCodeVerifier,
                    loginAccountRepository, userPasswordRepository, userApi);
        }

        @Test
        void unknown_identity_type_fails_before_any_id_is_generated() {
            assertThatThrownBy(() -> service.register(cmd("no_such_type", "x", null, null, "WEB", false, null)))
                    .isInstanceOf(RuntimeException.class); // 换成 IdentityType.fromCode 实际抛的异常

            verifyNoInteractions(authIdFactory, verificationCodeVerifier, loginAccountRepository, userApi);
        }

        @Test
        void missing_code_is_rejected_before_ids_are_generated() {
            assertThatThrownBy(() -> service.register(cmd("email", "bob@example.com", null, null, "WEB", false, null)))
                    .isInstanceOf(VerificationFailedException.class);

            verify(transactionTemplate, never()).executeWithoutResult(any());
            verifyNoInteractions(verificationCodeVerifier, authIdFactory,
                    loginAccountRepository, userPasswordRepository, userApi);
        }

        @Test
        void blank_code_is_rejected_before_ids_are_generated() {
            assertThatThrownBy(() -> service.register(cmd("phone", "13800138000", null, null, "SMS", false, "   ")))
                    .isInstanceOf(VerificationFailedException.class);

            verify(transactionTemplate, never()).executeWithoutResult(any());
            verifyNoInteractions(verificationCodeVerifier, authIdFactory,
                    loginAccountRepository, userPasswordRepository, userApi);
        }

        @Test
        void wrong_code_is_rejected_and_persists_nothing() {
            when(verificationCodeVerifier.verify(any(), anyString())).thenReturn(false);

            assertThatThrownBy(() -> service.register(cmd("email", "bob@example.com", null, null, "WEB", false, "000000")))
                    .isInstanceOf(VerificationFailedException.class);

            verify(transactionTemplate, never()).executeWithoutResult(any());
            verifyNoInteractions(authIdFactory, loginAccountRepository, userPasswordRepository, userApi);
        }

        @Test
        void account_save_failure_stops_password_and_user_creation() {
            doThrow(new IllegalStateException("duplicate identifier"))
                    .when(loginAccountRepository).save(any());

            assertThatThrownBy(() -> service.register(cmd("username", "alice", PASSWORD, null, "WEB", false, null)))
                    .isInstanceOf(IllegalStateException.class);

            verifyNoInteractions(userPasswordRepository, userApi);
        }

        @Test
        void create_user_failure_propagates() {
            doThrow(new IllegalStateException("user api down"))
                    .when(userApi).createUser(anyLong(), anyString(), anyString());

            assertThatThrownBy(() -> service.register(cmd("username", "alice", PASSWORD, null, "WEB", false, null)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("user api down");
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