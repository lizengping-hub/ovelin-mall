package com.ovelin.mall.auth.infrastructure.persistence.converter;

import com.ovelin.mall.auth.domain.model.aggregate.UserPassword;
import com.ovelin.mall.auth.infrastructure.persistence.convertor.UserPasswordPOConverter;
import com.ovelin.mall.auth.infrastructure.persistence.po.UserPasswordPO;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserPasswordPOConverterTest {

    private static final ShardedId USER_ID = ShardedId.of(1, 200L);

    private static final String PASSWORD_HASH = "encoded-password-hash";

    private static final Instant PASSWORD_CHANGED_AT =
            Instant.parse("2026-01-01T10:00:00Z");

    private static final Instant LOCKED_UNTIL =
            Instant.parse("2026-01-02T10:00:00Z");

    @Test
    @DisplayName("toDomain：正确转换所有字段")
    void toDomainConvertsAllFields() {
        UserPasswordPO po = new UserPasswordPO();
        po.setUserId(USER_ID.value());
        po.setEncodedPassword(PASSWORD_HASH);
        po.setPasswordChangedAt(PASSWORD_CHANGED_AT);
        po.setFailedAttempts(3);
        po.setLockedUntil(LOCKED_UNTIL);

        UserPassword password = UserPasswordPOConverter.toDomain(po);

        assertThat(password.getUserId()).isEqualTo(USER_ID);
        assertThat(password.getEncodedPassword()).isEqualTo(PASSWORD_HASH);
        assertThat(password.getPasswordChangedAt()).isEqualTo(PASSWORD_CHANGED_AT);
        assertThat(password.getFailedAttempts()).isEqualTo(3);
        assertThat(password.getLockedUntil()).isEqualTo(LOCKED_UNTIL);
    }

    @Test
    @DisplayName("toPO：正确转换所有字段")
    void toPOConvertsAllFields() {
        UserPassword password = UserPassword.reconstitute(
                USER_ID,
                PASSWORD_HASH,
                PASSWORD_CHANGED_AT,
                3,
                LOCKED_UNTIL
        );

        UserPasswordPO po = UserPasswordPOConverter.toPO(password);

        assertThat(po.getUserId()).isEqualTo(USER_ID.value());
        assertThat(po.getEncodedPassword()).isEqualTo(PASSWORD_HASH);
        assertThat(po.getPasswordChangedAt()).isEqualTo(PASSWORD_CHANGED_AT);
        assertThat(po.getFailedAttempts()).isEqualTo(3);
        assertThat(po.getLockedUntil()).isEqualTo(LOCKED_UNTIL);
    }

    @Test
    @DisplayName("toDomain：未锁定时保留 null")
    void toDomainAllowsNullLockedUntil() {
        UserPasswordPO po = new UserPasswordPO();
        po.setUserId(USER_ID.value());
        po.setEncodedPassword(PASSWORD_HASH);
        po.setPasswordChangedAt(PASSWORD_CHANGED_AT);
        po.setFailedAttempts(0);
        po.setLockedUntil(null);

        UserPassword password = UserPasswordPOConverter.toDomain(po);

        assertThat(password.getLockedUntil()).isNull();
        assertThat(password.getFailedAttempts()).isZero();
    }

    @Test
    @DisplayName("toPO：未锁定时保留 null")
    void toPOAllowsNullLockedUntil() {
        UserPassword password = UserPassword.reconstitute(
                USER_ID,
                PASSWORD_HASH,
                PASSWORD_CHANGED_AT,
                0,
                null
        );

        UserPasswordPO po = UserPasswordPOConverter.toPO(password);

        assertThat(po.getLockedUntil()).isNull();
        assertThat(po.getFailedAttempts()).isZero();
    }
}