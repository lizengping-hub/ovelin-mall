package com.ovelin.mall.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ovelin.mall.auth.infrastructure.persistence.AuthIntegrationTestSupport;
import com.ovelin.mall.auth.infrastructure.persistence.po.UserMfaFactorPO;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class UserUserMfaFactorMapperIT extends AuthIntegrationTestSupport {

    @Autowired
    UserMfaFactorMapper mapper;

    @Test
    @DisplayName("短信因子带identityId往返")
    void smsFactorWithIdentityIdRoundTrip() {
        ShardedId userId = newId("user", 0);
        ShardedId identityId = newId("user_identity", 0);
        UserMfaFactorPO po = factor(newId("user_mfa_factor", 0), userId, "sms");
        po.setIdentityId(identityId.value());
        po.setStatus(1);
        po.setVerifiedAt(now());
        mapper.insert(po);

        UserMfaFactorPO actual = mapper.selectByUserId(userId.value()).get(0);

        assertThat(actual.getFactorType()).isEqualTo("sms");
        assertThat(actual.getIdentityId()).isEqualTo(identityId.value());
        assertThat(actual.getSecretEncrypted()).isNull();
        assertThat(actual.getStatus()).isEqualTo(1);
        assertThat(actual.getVerifiedAt()).isEqualTo(po.getVerifiedAt());
    }

    @Test
    @DisplayName("totp因子的identityId为空_密钥原样保存")
    void totpFactorIdentityIdIsNullSecretSavedAsIs() {
        ShardedId userId = newId("user", 0);
        UserMfaFactorPO po = factor(newId("user_mfa_factor", 0), userId, "totp");
        po.setSecretEncrypted("encrypted-secret");
        mapper.insert(po);

        UserMfaFactorPO actual = mapper.selectByUserId(userId.value()).get(0);

        assertThat(actual.getIdentityId()).isNull();
        assertThat(actual.getSecretEncrypted()).isEqualTo("encrypted-secret");
        assertThat(actual.getStatus()).isZero();
    }

    @Test
    @DisplayName("一个用户可以有多个因子且互不串用户")
    void oneUserCanHaveMultipleFactorsWithoutCrossUserContamination() {
        ShardedId user1 = newId("user", 0);
        ShardedId user2 = newId("user", 0);
        mapper.insert(factor(newId("user_mfa_factor", 0), user1, "totp"));
        mapper.insert(factor(newId("user_mfa_factor", 0), user1, "recovery_code"));
        mapper.insert(factor(newId("user_mfa_factor", 0), user2, "totp"));

        assertThat(mapper.selectByUserId(user1.value())).hasSize(2);
        assertThat(mapper.selectByUserId(user2.value())).hasSize(1);
    }

    @Test
    @DisplayName("update能修改状态并写回null")
    void updateCanModifyStatusAndWriteBackNull() {
        ShardedId userId = newId("user", 0);
        UserMfaFactorPO po = factor(newId("user_mfa_factor", 0), userId, "totp");
        po.setStatus(1);
        po.setLastUsedAt(now());
        mapper.insert(po);

        mapper.update(null, Wrappers.<UserMfaFactorPO>lambdaUpdate()
                .set(UserMfaFactorPO::getStatus, 2)
                .set(UserMfaFactorPO::getLastUsedAt, null)
                .eq(UserMfaFactorPO::getId, po.getId())
                .eq(UserMfaFactorPO::getUserId, userId.value()));

        UserMfaFactorPO actual = mapper.selectByUserId(userId.value()).get(0);
        assertThat(actual.getStatus()).isEqualTo(2);
        assertThat(actual.getLastUsedAt()).isNull();
    }

    @Test
    @DisplayName("deleteByUserId只删除该用户")
    void deleteByUserIdOnlyDeletesTargetUser() {
        ShardedId user1 = newId("user", 0);
        ShardedId user2 = newId("user", 0);
        mapper.insert(factor(newId("user_mfa_factor", 0), user1, "totp"));
        mapper.insert(factor(newId("user_mfa_factor", 0), user1, "recovery_code"));
        mapper.insert(factor(newId("user_mfa_factor", 0), user2, "totp"));

        assertThat(mapper.deleteByUserId(user1.value())).isEqualTo(2);

        assertThat(mapper.selectByUserId(user1.value())).isEmpty();
        assertThat(mapper.selectByUserId(user2.value())).hasSize(1);
    }

    private static UserMfaFactorPO factor(ShardedId id, ShardedId userId, String type) {
        UserMfaFactorPO po = new UserMfaFactorPO();
        po.setId(id.value());
        po.setUserId(userId.value());
        po.setFactorType(type);
        return po;
    }
}
