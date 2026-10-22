package com.ovelin.mall.auth.domain.model.agregate;

import com.ovelin.mall.auth.domain.exception.IdentifierAlreadyBoundException;
import com.ovelin.mall.auth.domain.model.aggregate.LoginAccount;
import com.ovelin.mall.auth.domain.model.aggregate.UserMfaFactor;
import com.ovelin.mall.auth.domain.model.aggregate.UserIdentity;
import com.ovelin.mall.auth.domain.model.enums.MfaFactorStatus;
import com.ovelin.mall.auth.domain.model.enums.MfaFactorType;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifier;
import com.ovelin.mall.auth.domain.model.identityIdentifier.PhoneIdentifier;
import com.ovelin.mall.auth.domain.model.identityIdentifier.UserNameIdentifier;
import com.ovelin.mall.common.exception.global.InvalidArgumentException;
import com.ovelin.mall.common.exception.global.InvalidStatusException;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.ovelin.mall.common.sharding.core.ov.ShardedId.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAccountTest {

    private static final Instant NOW = Instant.parse("2026-10-14T00:00:00Z");
    private static final IdentityIdentifier PHONE_IDENTITY_IDENTIFIER = PhoneIdentifier.parseRawInput("13800000000");
    private static final IdentityIdentifier USERNAME_IDENTITY_IDENTIFIER = UserNameIdentifier.from("alice");
    private static final ShardedId USER = of(1, 100);
    private static final ShardedId USERNAME_ID = of(1, 101);
    private static final ShardedId PHONE_ID = of(1, 102);
    private static final ShardedId EMAIL_ID = of(1, 103);
    private static final ShardedId FACTOR_1 = of(1, 201);
    private static final ShardedId FACTOR_2 = of(1, 202);
    private static final ShardedId OTHER_SHARD = of(2, 999);

    /** 刚注册：只有一个 username 身份，变更集已清空（相当于从库里还原出来） */
    private static LoginAccount persisted() {
        LoginAccount account = LoginAccount.open(USER, USERNAME_ID, USERNAME_IDENTITY_IDENTIFIER);
        account.clearChanges();
        return account;
    }

    /** 在 persisted() 基础上再绑定一个已验证的手机号，并清空变更集 */
    private static LoginAccount persistedWithVerifiedPhone() {
        LoginAccount account = persisted();
        account.bindIdentity(PHONE_ID, PHONE_IDENTITY_IDENTIFIER);
        account.verifyIdentity(PHONE_ID, NOW);
        account.clearChanges();
        return account;
    }

    @Nested
    class Open {

        @Test
        void createsAccountWithFirstIdentityAsAddedChange() {
            LoginAccount account = LoginAccount.open(USER, USERNAME_ID, USERNAME_IDENTITY_IDENTIFIER);

            assertThat(account.getUserId()).isEqualTo(USER);
            assertThat(account.getIdentities()).hasSize(1);
            assertThat(account.addedIdentities()).extracting(UserIdentity::getId).containsExactly(USERNAME_ID);
            assertThat(account.getFactors()).isEmpty();
            assertThat(account.hasChanges()).isTrue();
        }

        @Test
        void rejectsIdentityOnDifferentShard() {
            assertThatThrownBy(() -> LoginAccount.open(USER, OTHER_SHARD, USERNAME_IDENTITY_IDENTIFIER))
                    .isInstanceOf(InvalidArgumentException.class);
        }
    }

    @Nested
    class BindIdentity {

        @Test
        void addsUnverifiedIdentityAndRecordsAddedChange() {
            LoginAccount account = persisted();

            UserIdentity phone = account.bindIdentity(PHONE_ID,PHONE_IDENTITY_IDENTIFIER);

            assertThat(phone.isVerified()).isFalse();
            assertThat(account.getIdentities()).hasSize(2);
            assertThat(account.addedIdentities()).extracting(UserIdentity::getId).containsExactly(PHONE_ID);
        }

        @Test
        void rejectsDuplicateTypeAndIdentifier() {
            LoginAccount account = persistedWithVerifiedPhone();

            assertThatThrownBy(() -> account.bindIdentity(EMAIL_ID, PHONE_IDENTITY_IDENTIFIER))
                    .isInstanceOf(IdentifierAlreadyBoundException.class);
        }

        @Test
        void rejectsIdentityOnDifferentShard() {
            LoginAccount account = persisted();

            assertThatThrownBy(() -> account.bindIdentity(OTHER_SHARD, PHONE_IDENTITY_IDENTIFIER))
                    .isInstanceOf(InvalidArgumentException.class);
        }
    }

    @Nested
    class VerifyAndLogin {

        @Test
        void verifyMarksIdentityDirtyOnlyOnce() {
            LoginAccount account = persisted();
            account.bindIdentity(PHONE_ID, PHONE_IDENTITY_IDENTIFIER);
            account.clearChanges();

            account.verifyIdentity(PHONE_ID, NOW);
            assertThat(account.dirtyIdentities()).extracting(UserIdentity::getId).containsExactly(PHONE_ID);

            account.clearChanges();
            account.verifyIdentity(PHONE_ID, NOW.plusSeconds(60));
            assertThat(account.hasChanges()).isFalse();
        }

        @Test
        void changesOnNewlyAddedIdentityStayInAddedNotDirty() {
            LoginAccount account = persisted();
            account.bindIdentity(PHONE_ID, PHONE_IDENTITY_IDENTIFIER);

            account.verifyIdentity(PHONE_ID, NOW);

            assertThat(account.addedIdentities()).hasSize(1);
            assertThat(account.dirtyIdentities()).isEmpty();
        }

        @Test
        void recordLoginMarksIdentityDirty() {
            LoginAccount account = persisted();

            account.recordLogin(USERNAME_ID, NOW);

            assertThat(account.dirtyIdentities()).extracting(UserIdentity::getId).containsExactly(USERNAME_ID);
            assertThat(account.findIdentity(USERNAME_ID).orElseThrow().getLastLoginAt()).isEqualTo(NOW);
        }

        @Test
        void unknownIdentityIsRejected() {
            LoginAccount account = persisted();

            assertThatThrownBy(() -> account.verifyIdentity(EMAIL_ID, NOW))
                    .isInstanceOf(InvalidArgumentException.class);
        }
    }

    @Nested
    class UnbindIdentity {

        @Test
        void lastIdentityCannotBeUnbound() {
            LoginAccount account = persisted();

            assertThatThrownBy(() -> account.unbindIdentity(USERNAME_ID))
                    .isInstanceOf(InvalidArgumentException.class);
            assertThat(account.getIdentities()).hasSize(1);
        }

        @Test
        void disablesReferencingFactorsBeforeRemovingIdentity() {
            LoginAccount account = persistedWithVerifiedPhone();
            account.addIdentityFactor(FACTOR_1, PHONE_ID);
            account.enableFactor(FACTOR_1, NOW);
            account.addSecretFactor(FACTOR_2, MfaFactorType.TOTP, "secret");
            account.enableFactor(FACTOR_2, NOW);
            account.clearChanges();

            account.unbindIdentity(PHONE_ID);

            assertThat(account.removedIdentities()).extracting(UserIdentity::getId).containsExactly(PHONE_ID);
            assertThat(account.dirtyFactors()).extracting(UserMfaFactor::getId).containsExactly(FACTOR_1);
            assertThat(account.dirtyFactors().get(0).getStatus()).isEqualTo(MfaFactorStatus.DISABLED);
            // 不引用该身份的因子保持启用
            assertThat(account.enabledFactors()).extracting(UserMfaFactor::getId).containsExactly(FACTOR_2);
        }

        @Test
        void unbindingNewlyBoundIdentityCancelsOut() {
            LoginAccount account = persisted();
            account.bindIdentity(PHONE_ID, PHONE_IDENTITY_IDENTIFIER);

            account.unbindIdentity(PHONE_ID);

            assertThat(account.getIdentities()).hasSize(1);
            assertThat(account.hasChanges()).isFalse();
        }

        @Test
        void unknownIdentityIsRejected() {
            LoginAccount account = persistedWithVerifiedPhone();

            assertThatThrownBy(() -> account.unbindIdentity(EMAIL_ID))
                    .isInstanceOf(InvalidArgumentException.class);
        }
    }

    @Nested
    class IdentityFactors {

        @Test
        void requiresVerifiedPhoneOrEmail() {
            LoginAccount account = persisted();
            account.bindIdentity(PHONE_ID, PHONE_IDENTITY_IDENTIFIER);

            assertThatThrownBy(() -> account.addIdentityFactor(FACTOR_1, PHONE_ID))
                    .isInstanceOf(IllegalStateException.class);

            account.verifyIdentity(PHONE_ID, NOW);
            UserMfaFactor factor = account.addIdentityFactor(FACTOR_1, PHONE_ID);

            assertThat(factor.getType()).isEqualTo(MfaFactorType.SMS);
            assertThat(factor.getIdentityId()).isEqualTo(PHONE_ID);
            assertThat(factor.getStatus()).isEqualTo(MfaFactorStatus.PENDING);
            assertThat(account.addedFactors()).extracting(UserMfaFactor::getId).containsExactly(FACTOR_1);
        }

        @Test
        void usernameIdentityCannotBeUsedEvenWhenVerified() {
            LoginAccount account = persisted();
            account.verifyIdentity(USERNAME_ID, NOW);

            assertThatThrownBy(() -> account.addIdentityFactor(FACTOR_1, USERNAME_ID))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void rejectsSecondActiveFactorOnSameIdentityButAllowsAfterDisable() {
            LoginAccount account = persistedWithVerifiedPhone();
            account.addIdentityFactor(FACTOR_1, PHONE_ID);

            assertThatThrownBy(() -> account.addIdentityFactor(FACTOR_2, PHONE_ID))
                    .isInstanceOf(InvalidStatusException.class);

            account.disableFactor(FACTOR_1);
            assertThat(account.addIdentityFactor(FACTOR_2, PHONE_ID).getId()).isEqualTo(FACTOR_2);
        }

        @Test
        void cannotBeEnabledAfterItsIdentityWasUnbound() {
            LoginAccount account = persistedWithVerifiedPhone();
            account.addIdentityFactor(FACTOR_1, PHONE_ID);
            account.unbindIdentity(PHONE_ID);

            assertThatThrownBy(() -> account.enableFactor(FACTOR_1, NOW))
                    .isInstanceOf(InvalidArgumentException.class);
        }
    }

    @Nested
    class SecretFactors {

        @Test
        void addsTotpFactorPending() {
            LoginAccount account = persisted();

            UserMfaFactor factor = account.addSecretFactor(FACTOR_1, MfaFactorType.TOTP, "encrypted");

            assertThat(factor.getSecretEncrypted()).isEqualTo("encrypted");
            assertThat(factor.getIdentityId()).isNull();
            assertThat(factor.getStatus()).isEqualTo(MfaFactorStatus.PENDING);
        }

        @Test
        void smsAndEmailMustUseIdentityFactor() {
            LoginAccount account = persisted();

            assertThatThrownBy(() -> account.addSecretFactor(FACTOR_1, MfaFactorType.SMS, "x"))
                    .isInstanceOf(InvalidArgumentException.class);
        }

        @Test
        void rejectsFactorIdOnDifferentShard() {
            LoginAccount account = persisted();

            assertThatThrownBy(() -> account.addSecretFactor(OTHER_SHARD, MfaFactorType.TOTP, "x"))
                    .isInstanceOf(InvalidArgumentException.class);
        }
    }

    @Nested
    class FactorLifecycle {

        @Test
        void enableSetsStatusVerifiedAtAndMarksDirty() {
            LoginAccount account = persisted();
            account.addSecretFactor(FACTOR_1, MfaFactorType.TOTP, "s");
            account.clearChanges();

            account.enableFactor(FACTOR_1, NOW);

            UserMfaFactor factor = account.dirtyFactors().get(0);
            assertThat(factor.getStatus()).isEqualTo(MfaFactorStatus.ENABLED);
            assertThat(factor.getVerifiedAt()).isEqualTo(NOW);
            assertThat(account.requiresMfa()).isTrue();
        }

        @Test
        void requiresMfaOnlyCountsEnabledFactors() {
            LoginAccount account = persisted();
            account.addSecretFactor(FACTOR_1, MfaFactorType.TOTP, "s");
            assertThat(account.requiresMfa()).isFalse();

            account.enableFactor(FACTOR_1, NOW);
            assertThat(account.requiresMfa()).isTrue();

            account.disableFactor(FACTOR_1);
            assertThat(account.requiresMfa()).isFalse();
            assertThat(account.enabledFactors()).isEmpty();
        }

        @Test
        void disablingAlreadyDisabledFactorIsNotAChange() {
            LoginAccount account = persisted();
            account.addSecretFactor(FACTOR_1, MfaFactorType.TOTP, "s");
            account.disableFactor(FACTOR_1);
            account.clearChanges();

            account.disableFactor(FACTOR_1);

            assertThat(account.hasChanges()).isFalse();
        }

        @Test
        void recordFactorUsedMarksDirty() {
            LoginAccount account = persisted();
            account.addSecretFactor(FACTOR_1, MfaFactorType.TOTP, "s");
            account.clearChanges();

            account.recordFactorUsed(FACTOR_1, NOW);

            assertThat(account.dirtyFactors()).extracting(UserMfaFactor::getId).containsExactly(FACTOR_1);
            assertThat(account.dirtyFactors().get(0).getLastUsedAt()).isEqualTo(NOW);
        }

        @Test
        void removeFactorRecordsRemovedChange() {
            LoginAccount account = persisted();
            account.addSecretFactor(FACTOR_1, MfaFactorType.TOTP, "s");
            account.clearChanges();

            account.removeFactor(FACTOR_1);

            assertThat(account.getFactors()).isEmpty();
            assertThat(account.removedFactors()).extracting(UserMfaFactor::getId).containsExactly(FACTOR_1);
        }

        @Test
        void unknownFactorIsRejected() {
            LoginAccount account = persisted();

            assertThatThrownBy(() -> account.enableFactor(FACTOR_1, NOW))
                    .isInstanceOf(InvalidArgumentException.class);
        }
    }

    @Nested
    class Close {

        @Test
        void removesAllFactorsAndIdentities() {
            LoginAccount account = persistedWithVerifiedPhone();
            account.addIdentityFactor(FACTOR_1, PHONE_ID);
            account.addSecretFactor(FACTOR_2, MfaFactorType.TOTP, "s");
            account.clearChanges();

            account.close();

            assertThat(account.getIdentities()).isEmpty();
            assertThat(account.getFactors()).isEmpty();
            assertThat(account.removedFactors()).extracting(UserMfaFactor::getId)
                    .containsExactlyInAnyOrder(FACTOR_1, FACTOR_2);
            assertThat(account.removedIdentities()).extracting(UserIdentity::getId)
                    .containsExactlyInAnyOrder(USERNAME_ID, PHONE_ID);
        }

        @Test
        void closingNeverPersistedAccountLeavesNoChanges() {
            LoginAccount account = LoginAccount.open(USER, USERNAME_ID, USERNAME_IDENTITY_IDENTIFIER);

            account.close();

            assertThat(account.hasChanges()).isFalse();
        }
    }

    @Nested
    class ChangeTracking {

        @Test
        void reconstitutedAccountHasNoChanges() {
            assertThat(persisted().hasChanges()).isFalse();
        }

        @Test
        void clearChangesEmptiesAllChangeSets() {
            LoginAccount account = persisted();
            account.bindIdentity(PHONE_ID, PHONE_IDENTITY_IDENTIFIER);
            account.recordLogin(USERNAME_ID, NOW);
            account.addSecretFactor(FACTOR_1, MfaFactorType.TOTP, "s");

            account.clearChanges();

            assertThat(account.hasChanges()).isFalse();
            assertThat(account.addedIdentities()).isEmpty();
            assertThat(account.dirtyIdentities()).isEmpty();
            assertThat(account.addedFactors()).isEmpty();
        }
    }
}