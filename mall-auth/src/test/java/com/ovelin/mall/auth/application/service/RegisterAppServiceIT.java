package com.ovelin.mall.auth.application.service;

import com.ovelin.mall.auth.application.command.RegisterCommand;
import com.ovelin.mall.auth.application.port.VerificationCodeVerifier;
import com.ovelin.mall.auth.domain.exception.VerificationFailedException;
import com.ovelin.mall.auth.domain.repository.UserPasswordRepository;
import com.ovelin.mall.auth.infrastructure.persistence.AuthIntegrationTestSupport;
import com.ovelin.mall.user.api.UserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RegisterAppServiceIT extends AuthIntegrationTestSupport {

    @Autowired
    RegisterAppService registerAppService;

    // spy：默认走真实实现，需要时再 doThrow 注入故障
    @MockitoSpyBean
    UserApi userApi;
    @MockitoSpyBean
    UserPasswordRepository userPasswordRepository;
    @MockitoBean
    VerificationCodeVerifier verificationCodeVerifier;

    @BeforeEach
    void stubVerifier() {
        when(verificationCodeVerifier.verify(any(), anyString())).thenReturn(true);
    }


    // ---------- 成功路径 ----------

    @Test
    void register_username_persists_all_aggregates() {
        registerAppService.register(cmd("username", "alice", "Passw0rd!", null, "WEB", false, null));

        assertThat(count("user_identity")).isEqualTo(1);
        assertThat(count("user_password")).isEqualTo(1);
        assertThat(count("`user`")).isEqualTo(1);
        assertThat(count("identity_lookup")).isEqualTo(1);
        assertThat(confirmedLookupCount()).isEqualTo(1);

        // 密码必须是编码后的
        String hash = jdbcTemplate.queryForObject("select password_hash from user_password", String.class);
        assertThat(hash).isNotEqualTo("Passw0rd!");

        // 默认 displayName 由 userId.sequence 生成
        String displayName = jdbcTemplate.queryForObject("select display_name from `user`", String.class);
        assertThat(displayName).startsWith("user_name_");
    }

    @Test
    void register_email_without_password_creates_identity() {
        registerAppService.register(cmd("email", "bob@example.com", null, null, "WEB", false, null));

        assertThat(count("user_identity")).isEqualTo(1);
        assertThat(count("user_password")).isZero();
    }

    // ---------- 回滚：本次的重点 ----------

    @Test
    void create_user_failure_rolls_back_account_and_password() {
        doThrow(new IllegalStateException("user module down"))
                .when(userApi).createUser(anyLong(), anyString(), anyString());

        assertThatThrownBy(() ->
                registerAppService.register(cmd("username", "alice", "Passw0rd!", null, "WEB", true, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("user module down");

        assertNothingPersisted();
    }

    @Test
    void password_save_failure_rolls_back_account_and_skips_user_creation() {
        doThrow(new IllegalStateException("password table error"))
                .when(userPasswordRepository).add(any());

        assertThatThrownBy(() ->
                registerAppService.register(cmd("username", "alice", "Passw0rd!", null, "WEB", false, null)))
                .isInstanceOf(IllegalStateException.class);

        verify(userApi, never()).createUser(anyLong(), anyString(), anyString());
        assertNothingPersisted();
    }

    @Test
    void failed_registration_can_be_retried_with_same_identifier() {
        doThrow(new IllegalStateException("boom"))
                .doCallRealMethod()
                .when(userApi).createUser(anyLong(), anyString(), anyString());

        assertThatThrownBy(() ->
                registerAppService.register(cmd("username", "alice", "Passw0rd!", null, "WEB", false, null)))
                .isInstanceOf(IllegalStateException.class);

        // 第一次失败不能留下占位，把标识符占死
        registerAppService.register(cmd("username", "alice", "Passw0rd!", null, "WEB", false, null));

        assertThat(count("user_identity")).isEqualTo(1);
        assertThat(count("`user`")).isEqualTo(1);
        assertThat(confirmedLookupCount()).isEqualTo(1);
    }

    // ---------- 重复注册 ----------

    @Test
    void duplicate_identifier_is_rejected_and_first_registration_is_intact() {
        registerAppService.register(cmd("username", "alice", "Passw0rd!", null, "WEB", false, null));

        assertThatThrownBy(() ->
                registerAppService.register(cmd("username", "alice", "Other#123", null, "WEB", false, null)))
                .isInstanceOf(RuntimeException.class); // 换成实际的重复异常类型

        assertThat(count("user_identity")).isEqualTo(1);
        assertThat(count("user_password")).isEqualTo(1);
        assertThat(count("`user`")).isEqualTo(1);
        assertThat(confirmedLookupCount()).isEqualTo(1);
    }
    @Test
    void register_phone_with_mfa_inserts_identity_and_factor_rows() {
        registerAppService.register(cmd("phone", "13800138000", null, null, "SMS", true, "123456"));

        assertThat(count("user_mfa_factor")).isEqualTo(1);
        assertThat(count("user_identity")).isEqualTo(1);
    }

    @Test
    void wrong_code_leaves_no_trace_including_lookup_placeholder() {
        when(verificationCodeVerifier.verify(any(), anyString())).thenReturn(false);

        assertThatThrownBy(() ->
                registerAppService.register(cmd("email", "bob@example.com", null, null, "WEB", false, "000000")))
                .isInstanceOf(VerificationFailedException.class);

        assertNothingPersisted();
        assertThat(count("identity_lookup")).isZero();   // 验证在占位之前，不应留下占位
    }

    @Test
    void username_with_mfa_is_rejected_and_persists_nothing() {
        assertThatThrownBy(() ->
                registerAppService.register(cmd("username", "alice", "Passw0rd!", null, "WEB", true, null)))
                .isInstanceOf(IllegalArgumentException.class);

        assertNothingPersisted();
        assertThat(count("identity_lookup")).isZero();
    }
    // ---------- helpers ----------

    private void assertNothingPersisted() {
        assertThat(count("user_identity")).isZero();
        assertThat(count("user_password")).isZero();
        assertThat(count("user_mfa_factor")).isZero();
        assertThat(count("`user`")).isZero();
        assertThat(confirmedLookupCount()).isZero();
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
    }

    private int confirmedLookupCount() {
        // TODO: 'CONFIRMED' 换成 identity_lookup.status 的实际取值（可能是 code）
        return jdbcTemplate.queryForObject(
                "select count(*) from identity_lookup where status = ?", Integer.class, "CONFIRMED");
    }

    private RegisterCommand cmd(String type, String identifier, String password, String displayName,
                                String source, boolean mfa, String verifyCode) {
        return new RegisterCommand(type, identifier, password, displayName, source, mfa, verifyCode);
    }
}