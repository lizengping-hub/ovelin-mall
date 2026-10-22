package com.ovelin.mall.auth.infrastructure.persistence.converter;

import com.ovelin.mall.auth.domain.model.aggregate.UserMfaFactor;
import com.ovelin.mall.auth.domain.model.enums.MfaFactorStatus;
import com.ovelin.mall.auth.domain.model.enums.MfaFactorType;
import com.ovelin.mall.auth.infrastructure.persistence.convertor.MfaFactorPOConverter;
import com.ovelin.mall.auth.infrastructure.persistence.po.UserMfaFactorPO;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserMfaFactorPOConverterTest {

    private static final ShardedId FACTOR_ID = ShardedId.of(1, 100L);
    private static final ShardedId USER_ID = ShardedId.of(1, 200L);
    private static final ShardedId IDENTITY_ID = ShardedId.of(1, 300L);

    private static final Instant VERIFIED_AT =
            Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant LAST_USED_AT =
            Instant.parse("2026-01-02T10:00:00Z");

    @Test
    @DisplayName("toDomain：正确转换所有字段")
    void toDomainConvertsAllFields() {
        UserMfaFactorPO po = new UserMfaFactorPO();
        po.setId(FACTOR_ID.value());
        po.setUserId(USER_ID.value());
        po.setFactorType(MfaFactorType.TOTP.getCode());
        po.setIdentityId(IDENTITY_ID.value());
        po.setSecretEncrypted("encrypted-secret");
        po.setStatus(MfaFactorStatus.ENABLED.getCode());
        po.setVerifiedAt(VERIFIED_AT);
        po.setLastUsedAt(LAST_USED_AT);

        UserMfaFactor factor = MfaFactorPOConverter.toDomain(po);

        assertThat(factor.getId()).isEqualTo(FACTOR_ID);
        assertThat(factor.getUserId()).isEqualTo(USER_ID);
        assertThat(factor.getType()).isEqualTo(MfaFactorType.TOTP);
        assertThat(factor.getIdentityId()).isEqualTo(IDENTITY_ID);
        assertThat(factor.getSecretEncrypted()).isEqualTo("encrypted-secret");
        assertThat(factor.getStatus()).isEqualTo(MfaFactorStatus.ENABLED);
        assertThat(factor.getVerifiedAt()).isEqualTo(VERIFIED_AT);
        assertThat(factor.getLastUsedAt()).isEqualTo(LAST_USED_AT);
    }

    @Test
    @DisplayName("toDomain：identityId 为空时保留 null")
    void toDomainAllowsNullIdentityId() {
        UserMfaFactorPO po = new UserMfaFactorPO();
        po.setId(FACTOR_ID.value());
        po.setUserId(USER_ID.value());
        po.setFactorType(MfaFactorType.TOTP.getCode());
        po.setIdentityId(null);
        po.setSecretEncrypted("encrypted-secret");
        po.setStatus(MfaFactorStatus.ENABLED.getCode());

        UserMfaFactor factor = MfaFactorPOConverter.toDomain(po);

        assertThat(factor.getIdentityId()).isNull();
    }

    @Test
    @DisplayName("toPO：正确转换所有字段")
    void toPOConvertsAllFields() {
        UserMfaFactor factor = UserMfaFactor.reconstitute(
                FACTOR_ID,
                USER_ID,
                MfaFactorType.TOTP,
                IDENTITY_ID,
                "encrypted-secret",
                MfaFactorStatus.ENABLED,
                VERIFIED_AT,
                LAST_USED_AT
        );

        UserMfaFactorPO po = MfaFactorPOConverter.toPO(factor);

        assertThat(po.getId()).isEqualTo(FACTOR_ID.value());
        assertThat(po.getUserId()).isEqualTo(USER_ID.value());
        assertThat(po.getFactorType()).isEqualTo(MfaFactorType.TOTP.getCode());
        assertThat(po.getIdentityId()).isEqualTo(IDENTITY_ID.value());
        assertThat(po.getSecretEncrypted()).isEqualTo("encrypted-secret");
        assertThat(po.getStatus()).isEqualTo(MfaFactorStatus.ENABLED.getCode());
        assertThat(po.getVerifiedAt()).isEqualTo(VERIFIED_AT);
        assertThat(po.getLastUsedAt()).isEqualTo(LAST_USED_AT);
    }

    @Test
    @DisplayName("toPO：identityId 为空时保留 null")
    void toPOAllowsNullIdentityId() {
        UserMfaFactor factor = UserMfaFactor.reconstitute(
                FACTOR_ID,
                USER_ID,
                MfaFactorType.TOTP,
                null,
                "encrypted-secret",
                MfaFactorStatus.ENABLED,
                null,
                null
        );

        UserMfaFactorPO po = MfaFactorPOConverter.toPO(factor);

        assertThat(po.getIdentityId()).isNull();
        assertThat(po.getVerifiedAt()).isNull();
        assertThat(po.getLastUsedAt()).isNull();
    }
}