package com.ovelin.mall.auth.infrastructure.persistence.repository;

import com.ovelin.mall.auth.domain.exception.IdentifierAlreadyTakenException;
import com.ovelin.mall.auth.domain.model.aggregate.LoginAccount;
import com.ovelin.mall.auth.domain.model.aggregate.MfaFactor;
import com.ovelin.mall.auth.domain.model.aggregate.UserIdentity;
import com.ovelin.mall.auth.domain.model.enums.MfaFactorStatus;
import com.ovelin.mall.auth.domain.model.enums.MfaFactorType;
import com.ovelin.mall.auth.domain.model.identityIdentifier.EmailIdentifier;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifier;
import com.ovelin.mall.auth.domain.model.identityIdentifier.PhoneIdentifier;
import com.ovelin.mall.auth.infrastructure.persistence.AuthIntegrationTestSupport;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * LoginAccountRepositoryImpl 集成测试。
 *
 * <p>分片规则说明：
 * <ul>
 *   <li>注册时，userId 由 (identityType, identifier) 计算出的分片键生成，因此首次注册时所有数据（
 *       user_identity、identity_lookup）落在同一分片。</li>
 *   <li>identity_lookup 按 (identity_type, identifier) 分片，无法和其它表在同一本地事务中；
 *       仓储用"PENDING → ACTIVE"协议保证最终一致。</li>
 * </ul>
 *
 * <p>测试用 shard=0 生成 userId 和子实体 id，确保所有行都落在同一分片，从而让本地事务生效。
 */
class LoginAccountRepositoryImplIT extends AuthIntegrationTestSupport {

    private static final int SHARD = 0;

    private static final IdentityIdentifier PHONE_IDENTITY_IDENTIFIER =
            PhoneIdentifier.parseRawInput("13800000001");
    private static final IdentityIdentifier EMAIL_IDENTITY_IDENTIFIER =
            EmailIdentifier.from("test@example.com");

    @Autowired
    private LoginAccountRepositoryImpl repository;

    // ------------------------------------------------------------------ findByUserId

    @Test
    @DisplayName("查询不存在的 userId 应返回空")
    void findByUserId_userNotFound_returnsEmpty() {
        ShardedId missingId = newId("user", SHARD);

        Optional<LoginAccount> result = repository.findByUserId(missingId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("保存账号后能按 userId 还原出完整的身份信息")
    void findByUserId_afterSave_reconstructsAccount() {
        ShardedId userId = newId("user", SHARD);
        ShardedId identityId = newId("user_identity", SHARD);
        LoginAccount account = LoginAccount.open(userId, identityId, PHONE_IDENTITY_IDENTIFIER);
        repository.save(account);

        Optional<LoginAccount> loaded = repository.findByUserId(userId);

        assertThat(loaded).isPresent();
        LoginAccount la = loaded.get();
        assertThat(la.getUserId()).isEqualTo(userId);
        assertThat(la.getIdentities()).hasSize(1);
        UserIdentity identity = la.getIdentities().get(0);
        assertThat(identity.getType()).isEqualTo(PHONE_IDENTITY_IDENTIFIER.type());
        assertThat(identity.getIdentifier()).isEqualTo(PHONE_IDENTITY_IDENTIFIER.value());
        assertThat(identity.getVerifiedAt()).isNull();
        assertThat(identity.getLastLoginAt()).isNull();
    }

    @Test
    @DisplayName("保存多身份和已启用的 MFA 因子后，能完整还原")
    void findByUserId_multipleIdentitiesAndFactor_allReconstituted() {
        ShardedId userId = newId("user", SHARD);
        ShardedId phoneId = newId("user_identity", SHARD);
        ShardedId emailId = newId("user_identity", SHARD);
        ShardedId factorId = newId("user_mfa_factor", SHARD);

        LoginAccount account = LoginAccount.open(userId, phoneId, PHONE_IDENTITY_IDENTIFIER);
        account.verifyIdentity(phoneId, now());
        account.bindIdentity(emailId, EMAIL_IDENTITY_IDENTIFIER);
        account.verifyIdentity(emailId, now());
        MfaFactor factor = account.addIdentityFactor(factorId, phoneId);
        account.enableFactor(factor.getId(), now());
        repository.save(account);

        LoginAccount loaded = repository.findByUserId(userId).orElseThrow();
        assertThat(loaded.getIdentities()).hasSize(2);
        assertThat(loaded.getFactors()).hasSize(1);
        assertThat(loaded.getFactors().get(0).getStatus()).isEqualTo(MfaFactorStatus.ENABLED);
    }

    // ------------------------------------------------------------------ findUserIdByIdentity

    @Test
    @DisplayName("保存账号后，identity_lookup 进入 ACTIVE，能按标识路由到 userId")
    void findUserIdByIdentity_afterSave_routesToUserId() {
        ShardedId userId = newId("user", SHARD);
        ShardedId identityId = newId("user_identity", SHARD);
        LoginAccount account = LoginAccount.open(userId, identityId, PHONE_IDENTITY_IDENTIFIER);
        repository.save(account);

        Optional<ShardedId> found = repository.findUserIdByIdentity(PHONE_IDENTITY_IDENTIFIER);

        assertThat(found).contains(userId);
    }

    @Test
    @DisplayName("解绑身份后，该标识从 lookup 中移除；保留的标识仍可查")
    void findUserIdByIdentity_afterUnbind_removedIdentityNotRouteable() {
        ShardedId userId = newId("user", SHARD);
        ShardedId phoneId = newId("user_identity", SHARD);
        ShardedId emailId = newId("user_identity", SHARD);

        LoginAccount account = LoginAccount.open(userId, phoneId, PHONE_IDENTITY_IDENTIFIER);
        account.bindIdentity(emailId, EMAIL_IDENTITY_IDENTIFIER);
        repository.save(account);

        LoginAccount loaded = repository.findByUserId(userId).orElseThrow();
        loaded.unbindIdentity(phoneId);
        repository.save(loaded);

        assertThat(repository.findUserIdByIdentity(PHONE_IDENTITY_IDENTIFIER)).isEmpty();
        assertThat(repository.findUserIdByIdentity(EMAIL_IDENTITY_IDENTIFIER)).contains(userId);
    }

    // ------------------------------------------------------------------ save - 新增身份

    @Test
    @DisplayName("clearChanges 后再次 save 为幂等 no-op，不产生重复行")
    void save_afterClearChanges_isIdempotentNoOp() {
        ShardedId userId = newId("user", SHARD);
        ShardedId identityId = newId("user_identity", SHARD);
        LoginAccount account = LoginAccount.open(userId, identityId, PHONE_IDENTITY_IDENTIFIER);
        repository.save(account);
        repository.save(account); // 变更集已清空，应为 no-op

        assertThat(repository.findByUserId(userId).orElseThrow().getIdentities()).hasSize(1);
    }

    @Test
    @DisplayName("注册已被占用的标识时，抛出 IdentifierAlreadyTakenException")
    void save_duplicateIdentifier_throwsIdentifierAlreadyTakenException() {
        ShardedId userId1 = newId("user", SHARD);
        ShardedId identityId1 = newId("user_identity", SHARD);
        LoginAccount account1 = LoginAccount.open(userId1, identityId1, PHONE_IDENTITY_IDENTIFIER);
        repository.save(account1);

        ShardedId userId2 = newId("user", SHARD);
        ShardedId identityId2 = newId("user_identity", SHARD);
        LoginAccount account2 = LoginAccount.open(userId2, identityId2, PHONE_IDENTITY_IDENTIFIER);

        assertThatThrownBy(() -> repository.save(account2))
                .isInstanceOf(IdentifierAlreadyTakenException.class);
    }

    // ------------------------------------------------------------------ save - 修改身份

    @Test
    @DisplayName("验证身份后，verifiedAt 被持久化")
    void save_verifyIdentity_persistsVerifiedAt() {
        ShardedId userId = newId("user", SHARD);
        ShardedId identityId = newId("user_identity", SHARD);
        LoginAccount account = LoginAccount.open(userId, identityId, PHONE_IDENTITY_IDENTIFIER);
        repository.save(account);

        Instant verifiedAt = now();
        LoginAccount loaded = repository.findByUserId(userId).orElseThrow();
        loaded.verifyIdentity(identityId, verifiedAt);
        repository.save(loaded);

        UserIdentity identity = repository.findByUserId(userId).orElseThrow()
                .getIdentities().get(0);
        assertThat(identity.getVerifiedAt()).isEqualTo(verifiedAt);
    }

    @Test
    @DisplayName("记录登录时间后，lastLoginAt 被持久化")
    void save_recordLogin_persistsLastLoginAt() {
        ShardedId userId = newId("user", SHARD);
        ShardedId identityId = newId("user_identity", SHARD);
        LoginAccount account = LoginAccount.open(userId, identityId, PHONE_IDENTITY_IDENTIFIER);
        repository.save(account);

        Instant loginAt = now();
        LoginAccount loaded = repository.findByUserId(userId).orElseThrow();
        loaded.recordLogin(identityId, loginAt);
        repository.save(loaded);

        UserIdentity identity = repository.findByUserId(userId).orElseThrow()
                .getIdentities().get(0);
        assertThat(identity.getLastLoginAt()).isEqualTo(loginAt);
    }

    // ------------------------------------------------------------------ save - 解绑身份

    @Test
    @DisplayName("解绑身份后，对应身份行被删除，其余身份保留")
    void save_unbindIdentity_removesIdentityRow() {
        ShardedId userId = newId("user", SHARD);
        ShardedId phoneId = newId("user_identity", SHARD);
        ShardedId emailId = newId("user_identity", SHARD);
        LoginAccount account = LoginAccount.open(userId, phoneId, PHONE_IDENTITY_IDENTIFIER);
        account.bindIdentity(emailId, EMAIL_IDENTITY_IDENTIFIER);
        repository.save(account);

        LoginAccount loaded = repository.findByUserId(userId).orElseThrow();
        loaded.unbindIdentity(phoneId);
        repository.save(loaded);

        LoginAccount reloaded = repository.findByUserId(userId).orElseThrow();
        assertThat(reloaded.getIdentities()).hasSize(1);
        assertThat(reloaded.getIdentities().get(0).getType())
                .isEqualTo(EMAIL_IDENTITY_IDENTIFIER.type());
    }

    @Test
    @DisplayName("解绑有关联 MFA 因子的身份时，关联因子被置为 DISABLED")
    void save_unbindIdentityWithFactor_disablesReferencingFactor() {
        ShardedId userId = newId("user", SHARD);
        ShardedId phoneId = newId("user_identity", SHARD);
        ShardedId emailId = newId("user_identity", SHARD);
        ShardedId factorId = newId("user_mfa_factor", SHARD);

        LoginAccount account = LoginAccount.open(userId, phoneId, PHONE_IDENTITY_IDENTIFIER);
        account.verifyIdentity(phoneId, now());
        account.bindIdentity(emailId, EMAIL_IDENTITY_IDENTIFIER);
        MfaFactor factor = account.addIdentityFactor(factorId, phoneId);
        account.enableFactor(factor.getId(), now());
        repository.save(account);

        LoginAccount loaded = repository.findByUserId(userId).orElseThrow();
        loaded.unbindIdentity(phoneId);
        repository.save(loaded);

        LoginAccount reloaded = repository.findByUserId(userId).orElseThrow();
        assertThat(reloaded.getFactors()).hasSize(1);
        assertThat(reloaded.getFactors().get(0).getStatus()).isEqualTo(MfaFactorStatus.DISABLED);
    }

    // ------------------------------------------------------------------ save - MFA 因子增删改

    @Test
    @DisplayName("新增 TOTP 因子后，密文、类型和 PENDING 状态能被还原")
    void save_addTotpFactor_reconstructedCorrectly() {
        ShardedId userId = newId("user", SHARD);
        ShardedId identityId = newId("user_identity", SHARD);
        ShardedId factorId = newId("user_mfa_factor", SHARD);
        LoginAccount account = LoginAccount.open(userId, identityId, PHONE_IDENTITY_IDENTIFIER);
        account.addSecretFactor(factorId, MfaFactorType.TOTP, "encrypted-secret");
        repository.save(account);

        LoginAccount loaded = repository.findByUserId(userId).orElseThrow();
        assertThat(loaded.getFactors()).hasSize(1);
        MfaFactor factor = loaded.getFactors().get(0);
        assertThat(factor.getType()).isEqualTo(MfaFactorType.TOTP);
        assertThat(factor.getSecretEncrypted()).isEqualTo("encrypted-secret");
        assertThat(factor.getStatus()).isEqualTo(MfaFactorStatus.PENDING);
    }

    @Test
    @DisplayName("启用 MFA 因子后，状态变为 ENABLED，verifiedAt 被持久化")
    void save_enableFactor_persistsStatusAndVerifiedAt() {
        ShardedId userId = newId("user", SHARD);
        ShardedId phoneId = newId("user_identity", SHARD);
        ShardedId factorId = newId("user_mfa_factor", SHARD);

        LoginAccount account = LoginAccount.open(userId, phoneId, PHONE_IDENTITY_IDENTIFIER);
        account.verifyIdentity(phoneId, now());
        MfaFactor factor = account.addIdentityFactor(factorId, phoneId);
        repository.save(account);

        Instant enabledAt = now();
        LoginAccount loaded = repository.findByUserId(userId).orElseThrow();
        loaded.enableFactor(factor.getId(), enabledAt);
        repository.save(loaded);

        MfaFactor reloaded = repository.findByUserId(userId).orElseThrow()
                .getFactors().get(0);
        assertThat(reloaded.getStatus()).isEqualTo(MfaFactorStatus.ENABLED);
        assertThat(reloaded.getVerifiedAt()).isEqualTo(enabledAt);
    }

    @Test
    @DisplayName("停用 MFA 因子后，状态变为 DISABLED 并被持久化")
    void save_disableFactor_persistsDisabledStatus() {
        ShardedId userId = newId("user", SHARD);
        ShardedId phoneId = newId("user_identity", SHARD);
        ShardedId factorId = newId("user_mfa_factor", SHARD);

        LoginAccount account = LoginAccount.open(userId, phoneId, PHONE_IDENTITY_IDENTIFIER);
        account.verifyIdentity(phoneId, now());
        MfaFactor factor = account.addIdentityFactor(factorId, phoneId);
        account.enableFactor(factor.getId(), now());
        repository.save(account);

        LoginAccount loaded = repository.findByUserId(userId).orElseThrow();
        loaded.disableFactor(factor.getId());
        repository.save(loaded);

        assertThat(repository.findByUserId(userId).orElseThrow()
                .getFactors().get(0).getStatus())
                .isEqualTo(MfaFactorStatus.DISABLED);
    }

    @Test
    @DisplayName("记录因子使用时间后，lastUsedAt 被持久化")
    void save_recordFactorUsed_persistsLastUsedAt() {
        ShardedId userId = newId("user", SHARD);
        ShardedId phoneId = newId("user_identity", SHARD);
        ShardedId factorId = newId("user_mfa_factor", SHARD);

        LoginAccount account = LoginAccount.open(userId, phoneId, PHONE_IDENTITY_IDENTIFIER);
        account.verifyIdentity(phoneId, now());
        MfaFactor factor = account.addIdentityFactor(factorId, phoneId);
        account.enableFactor(factor.getId(), now());
        repository.save(account);

        Instant usedAt = now();
        LoginAccount loaded = repository.findByUserId(userId).orElseThrow();
        loaded.recordFactorUsed(factor.getId(), usedAt);
        repository.save(loaded);

        assertThat(repository.findByUserId(userId).orElseThrow()
                .getFactors().get(0).getLastUsedAt())
                .isEqualTo(usedAt);
    }

    @Test
    @DisplayName("删除 MFA 因子后，因子行从数据库中移除")
    void save_removeFactor_deletesFactorRow() {
        ShardedId userId = newId("user", SHARD);
        ShardedId identityId = newId("user_identity", SHARD);
        ShardedId factorId = newId("user_mfa_factor", SHARD);
        LoginAccount account = LoginAccount.open(userId, identityId, PHONE_IDENTITY_IDENTIFIER);
        account.addSecretFactor(factorId, MfaFactorType.TOTP, "enc");
        repository.save(account);

        LoginAccount loaded = repository.findByUserId(userId).orElseThrow();
        loaded.removeFactor(factorId);
        repository.save(loaded);

        assertThat(repository.findByUserId(userId).orElseThrow().getFactors()).isEmpty();
    }

    // ------------------------------------------------------------------ save - 注销

    @Test
    @DisplayName("注销账号后，所有身份和因子行被删除，lookup 中的标识也被释放")
    void save_closeAccount_removesAllRowsAndReleasesLookup() {
        ShardedId userId = newId("user", SHARD);
        ShardedId phoneId = newId("user_identity", SHARD);
        ShardedId emailId = newId("user_identity", SHARD);
        ShardedId factorId = newId("user_mfa_factor", SHARD);

        LoginAccount account = LoginAccount.open(userId, phoneId, PHONE_IDENTITY_IDENTIFIER);
        account.verifyIdentity(phoneId, now());
        account.bindIdentity(emailId, EMAIL_IDENTITY_IDENTIFIER);
        account.addSecretFactor(factorId, MfaFactorType.TOTP, "enc");
        repository.save(account);

        LoginAccount loaded = repository.findByUserId(userId).orElseThrow();
        loaded.close();
        repository.save(loaded);

        assertThat(repository.findByUserId(userId)).isEmpty();
        assertThat(repository.findUserIdByIdentity(PHONE_IDENTITY_IDENTIFIER)).isEmpty();
        assertThat(repository.findUserIdByIdentity(EMAIL_IDENTITY_IDENTIFIER)).isEmpty();
    }

    // ------------------------------------------------------------------ save - 变更集

    @Test
    @DisplayName("save 成功后变更集被清空，hasChanges() 返回 false")
    void save_afterSuccess_changesCleared() {
        ShardedId userId = newId("user", SHARD);
        ShardedId identityId = newId("user_identity", SHARD);
        LoginAccount account = LoginAccount.open(userId, identityId, PHONE_IDENTITY_IDENTIFIER);
        repository.save(account);

        assertThat(account.hasChanges()).isFalse();
    }
}
