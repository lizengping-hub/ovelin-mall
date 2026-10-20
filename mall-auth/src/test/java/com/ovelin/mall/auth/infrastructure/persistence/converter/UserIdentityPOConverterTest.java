package com.ovelin.mall.auth.infrastructure.persistence.converter;

import com.ovelin.mall.auth.domain.model.aggregate.UserIdentity;
import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import com.ovelin.mall.auth.infrastructure.persistence.convertor.UserIdentityPOConverter;
import com.ovelin.mall.auth.infrastructure.persistence.po.IdentityLookupPO;
import com.ovelin.mall.auth.infrastructure.persistence.po.UserIdentityPO;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserIdentityPOConverterTest {

    private static final ShardedId ID = ShardedId.of(1, 100L);
    private static final ShardedId USER_ID = ShardedId.of(1, 200L);

    private static final String IDENTIFIER = "user@example.com";

    private static final Instant VERIFIED_AT =
            Instant.parse("2026-01-01T10:00:00Z");

    private static final Instant LAST_LOGIN_AT =
            Instant.parse("2026-01-02T10:00:00Z");

    @Test
    @DisplayName("toDomain：正确转换所有字段")
    void toDomainConvertsAllFields() {
        UserIdentityPO po = new UserIdentityPO();
        po.setId(ID.value());
        po.setUserId(USER_ID.value());
        po.setIdentityType(IdentityType.EMAIL.getCode());
        po.setIdentifier(IDENTIFIER);
        po.setVerifiedAt(VERIFIED_AT);
        po.setLastLoginAt(LAST_LOGIN_AT);

        UserIdentity identity = UserIdentityPOConverter.toDomain(po);

        assertThat(identity.getId()).isEqualTo(ID);
        assertThat(identity.getUserId()).isEqualTo(USER_ID);
        assertThat(identity.getType()).isEqualTo(IdentityType.EMAIL);
        assertThat(identity.getIdentifier()).isEqualTo(IDENTIFIER);
        assertThat(identity.getVerifiedAt()).isEqualTo(VERIFIED_AT);
        assertThat(identity.getLastLoginAt()).isEqualTo(LAST_LOGIN_AT);
    }

    @Test
    @DisplayName("toPO：正确转换所有字段")
    void toPOConvertsAllFields() {
        UserIdentity identity = UserIdentity.reconstitute(
                ID,
                USER_ID,
                IdentityType.EMAIL.getCode(),
                IDENTIFIER,
                VERIFIED_AT,
                LAST_LOGIN_AT
        );

        UserIdentityPO po = UserIdentityPOConverter.toPO(identity);

        assertThat(po.getId()).isEqualTo(ID.value());
        assertThat(po.getUserId()).isEqualTo(USER_ID.value());
        assertThat(po.getIdentityType()).isEqualTo(IdentityType.EMAIL.getCode());
        assertThat(po.getIdentifier()).isEqualTo(IDENTIFIER);
        assertThat(po.getVerifiedAt()).isEqualTo(VERIFIED_AT);
        assertThat(po.getLastLoginAt()).isEqualTo(LAST_LOGIN_AT);
    }

    @Test
    @DisplayName("toLookupPO：正确转换身份类型、标识符和用户 ID")
    void toLookupPOConvertsAllFields() {
        UserIdentity identity = UserIdentity.reconstitute(
                ID,
                USER_ID,
                IdentityType.EMAIL.getCode(),
                IDENTIFIER,
                VERIFIED_AT,
                LAST_LOGIN_AT
        );

        IdentityLookupPO po = UserIdentityPOConverter.toLookupPO(identity);

        assertThat(po.getIdentityType()).isEqualTo(IdentityType.EMAIL.getCode());
        assertThat(po.getIdentifier()).isEqualTo(IDENTIFIER);
        assertThat(po.getUserId()).isEqualTo(USER_ID.value());
    }

    @Test
    @DisplayName("toPO：未验证且从未登录的身份保留 null 时间")
    void toPOAllowsNullTimestamps() {
        UserIdentity identity = UserIdentity.reconstitute(
                ID,
                USER_ID,
                IdentityType.EMAIL.getCode(),
                IDENTIFIER,
                null,
                null
        );

        UserIdentityPO po = UserIdentityPOConverter.toPO(identity);

        assertThat(po.getVerifiedAt()).isNull();
        assertThat(po.getLastLoginAt()).isNull();
    }
}