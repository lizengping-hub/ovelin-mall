package com.ovelin.mall.auth.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ovelin.mall.auth.application.port.AuthIdFactory;
import com.ovelin.mall.auth.domain.exception.AccountStatusChangedException;
import com.ovelin.mall.auth.domain.exception.IdentifierAlreadyTakenException;
import com.ovelin.mall.auth.domain.exception.IdentifierNotOwnedByUserException;
import com.ovelin.mall.auth.domain.model.aggregate.LoginAccount;
import com.ovelin.mall.auth.domain.model.aggregate.UserMfaFactor;
import com.ovelin.mall.auth.domain.model.aggregate.UserIdentity;
import com.ovelin.mall.auth.domain.model.enums.IdentityLookupStatus;
import com.ovelin.mall.auth.domain.model.enums.MfaFactorType;
import com.ovelin.mall.auth.domain.model.identityIdentifier.EmailIdentifier;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifier;
import com.ovelin.mall.auth.domain.model.identityIdentifier.PhoneIdentifier;
import com.ovelin.mall.auth.infrastructure.persistence.AuthIntegrationTestSupport;
import com.ovelin.mall.auth.infrastructure.persistence.gateway.IdentityLookupGateway;
import com.ovelin.mall.auth.infrastructure.persistence.mapper.IdentityLookupMapper;
import com.ovelin.mall.auth.infrastructure.persistence.mapper.LoginAccountMapper;
import com.ovelin.mall.auth.infrastructure.persistence.mapper.UserIdentityMapper;
import com.ovelin.mall.auth.infrastructure.persistence.mapper.UserMfaFactorMapper;
import com.ovelin.mall.auth.infrastructure.persistence.po.IdentityLookupPO;
import com.ovelin.mall.auth.infrastructure.persistence.po.LoginAccountPO;
import com.ovelin.mall.auth.infrastructure.persistence.po.UserIdentityPO;
import com.ovelin.mall.auth.infrastructure.persistence.po.UserMfaFactorPO;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LoginAccountRepositoryImpl 集成测试。
 *
 * <p>仓储测试只关心"变更集 → SQL → 还原"这条映射，不重复验证 LoginAccount 的领域规则
 * （比如解绑身份会停用关联因子，那是 LoginAccount 单元测试的事）。
 *
 * <p>分片规则说明：
 * <ul>
 *   <li>注册时，userId 由 (identityType, identifier) 计算出的分片键生成（authIdFactory.newUserId），
 *       因此首次注册时 user_identity、identity_lookup 落在同一分片。</li>
 *   <li>identity_lookup 按 (identity_type, identifier) 分片，无法和其它表在同一本地事务中；
 *       仓储用"PENDING → ACTIVE"协议保证最终一致。</li>
 * </ul>
 *
 * <p>add 和 update 的关系：update 本身就能写入新账号（新增的身份/因子都在变更集里）。
 * add 只是在"单身份注册且身份分片与 userId 分片一致"时的快速路径（insertConfirmed，不走 reserve）；
 * 其它情况（多身份注册等）add 内部直接走和 update 相同的 reserve → 写行 → confirm 逻辑。
 * 所以 Add 里的用例只需要区分这两条路径，补偿、冲突这类逻辑放在 Update 里测一次即可。
 *
 * <p>Update 用例的约定：初始状态通过 add 落库，再 findByUserId → 修改 → update，
 * 这样每个用例测的才是"差异写入"。
 */
@Slf4j
class LoginAccountRepositoryImplIT extends AuthIntegrationTestSupport {

    private static final IdentityIdentifier PHONE =
            PhoneIdentifier.from("13800000001");
    private static final IdentityIdentifier EMAIL =
            EmailIdentifier.from("test@example.com");
    @Autowired
    AuthIdFactory authIdFactory;
    @MockitoSpyBean
    private LoginAccountMapper loginAccountMapper;
    @MockitoSpyBean
    private IdentityLookupMapper identityLookupMapper;
    @MockitoSpyBean
    private UserIdentityMapper identityMapper;
    @MockitoSpyBean
    private UserMfaFactorMapper factorMapper;
    /** 只用来区分 add 走的是快速路径（insertConfirmed）还是 reserve 路径 */
    @MockitoSpyBean
    private IdentityLookupGateway lookupGateway;

    @Autowired
    private LoginAccountRepositoryImpl repository;

    // ------------------------------------------------------------------ findByUserId

    @Nested
    @DisplayName("0. 查询用户Id findUserIdByIdentity")
    class FindUserIdByIdentity{
        @Test
        @DisplayName("查询不存在的身份的id返回空")
        void findUserIdOfIdentityNotExistReturnNull(){
            assertThat(repository.findUserIdByIdentity(PHONE)).isEmpty();
        }
        @Test
        @DisplayName("查询reserve 状态的存在的身份，无法获得用户id")
        void findUserIdOfIdentityReservedReturnNull(){
            IdentityLookupPO po = newIdentityLookupPO();
            po.setStatus(IdentityLookupStatus.RESERVED.getCode());
            identityLookupMapper.insert(po);

            assertThat(repository.findUserIdByIdentity(PHONE)).isEmpty();
        }

        @Test
        @DisplayName("查询active 状态的存在的身份，可以获得userId")
        void findUserIdOfIdentityActiveReturnUserId(){
            IdentityLookupPO po = newIdentityLookupPO();
            po.setStatus(IdentityLookupStatus.ACTIVE.getCode());
            identityLookupMapper.insert(po);
            assertThat(repository.findUserIdByIdentity(PHONE)).contains(ShardedId.fromId(po.getUserId()));
        }

    }
    @Nested
    @DisplayName("1. 根据用户Id查询LoginAccount findByUserId")
    class FindByUserId{
        @Test
        @DisplayName("查询不存在的 userId 应返回空")
        void findByUserId_userNotFound_returnsEmpty() {
            ShardedId missingId = newId("user");
            assertThat(repository.findByUserId(missingId)).isEmpty();
        }

        @Test
        @DisplayName("查询不存在的 userId 应返回空")
        void findByUserId_onlyHasIdentity_returnsEmpty() {
            ShardedId userId = newId("user");
            UserIdentityPO ui = newUserIdentityPO(userId);
            identityMapper.insert(ui);
            assertThat(repository.findByUserId(userId)).isEmpty();
        }


        @Test
        @DisplayName("能够 还原Identities")
        void findByUserId_getLoginAccountOfIdentities(){
            ShardedId userId = newId("user");

            LoginAccountPO accountPO = newLoginAccountPO(userId);
            loginAccountMapper.insert(accountPO);
            UserIdentityPO ui = newUserIdentityPO(userId);
            identityMapper.insert(ui);

            Optional<LoginAccount> accountOptional = repository.findByUserId(userId);
            assertThat(accountOptional).isPresent();
            LoginAccount account = accountOptional.get();
            // 只需检查有就行了，不需比较值
            assertThat(account.getUserId()).isEqualTo(userId);
            assertThat(account.getIdentities()).isNotEmpty();
            assertThat(account.getFactors()).isEmpty();
        }

        @Test
        @DisplayName("只有MfaFactors，不会还原")
        void findByUserId_userNotFoundOnlyMfaFactors(){
            ShardedId userId = newId("user");

            UserMfaFactorPO umfa = newUserMfaFactorPO(userId);
            factorMapper.insert(umfa);

            assertThat(repository.findByUserId(userId)).isEmpty();
        }


        @Test
        @DisplayName("能够 还原Factors")
        void findByUserId_getLoginAccountOfMfaFactors(){
            ShardedId userId = newId("user");
            LoginAccountPO accountPO = newLoginAccountPO(userId);
            loginAccountMapper.insert(accountPO);

            UserIdentityPO ui = newUserIdentityPO(userId);
            identityMapper.insert(ui);

            UserMfaFactorPO umfa = newUserMfaFactorPO(userId);
            factorMapper.insert(umfa);

            Optional<LoginAccount> accountOptional = repository.findByUserId(userId);
            assertThat(accountOptional).isPresent();
            LoginAccount account = accountOptional.get();
            // 只需检查有就行了，不需比较值
            assertThat(account.getUserId()).isEqualTo(userId);
            assertThat(account.getIdentities()).isNotEmpty();
            assertThat(account.getFactors()).isNotEmpty();
        }

    }


    @Nested
    @DisplayName("2. Add 新增")
    class Add{
        @Test
        @DisplayName("新增账号和查询")
        void addAndQuery(){
            LoginAccount account = newPhoneIdentityLoginAccount(false);

            repository.save(account);

            assertThat(account.getVersion()).isZero();
            assertThat(account.isNew()).isFalse();
            assertThat(account.hasChanges()).isFalse();
            assertThat(repository.findUserIdByIdentity(PHONE)).contains(account.getUserId());
            loadAndAssertLoginAccount(account);
        }
        @Test
        @DisplayName("新增账号和会清空变化记录")
        void addAndClearChanges(){
            LoginAccount account = newPhoneIdentityLoginAccount(false);
            repository.save(account);
            assertThat(account.hasChanges()).isFalse();
        }
        @Test
        @DisplayName("新增开启二次认证账号和查询")
        void addIncludeFactorAndQuery(){
            LoginAccount account = newPhoneIdentityLoginAccount(true);
            repository.save(account);
            assertThat(repository.findUserIdByIdentity(PHONE)).contains(account.getUserId());
            loadAndAssertLoginAccount(account);
        }

        @Test
        @DisplayName("单身份注册走快速路径：直接 insertConfirmed，不经过 reserve")
        void addSingleIdentityUsesFastPath() {
            LoginAccount account = newPhoneIdentityLoginAccount(false);

            repository.save(account);

            verify(lookupGateway).insertConfirmed(any(), any());
            verify(lookupGateway, never()).reserve(any(), any());
            assertThat(repository.findUserIdByIdentity(PHONE)).contains(account.getUserId());
        }

        @Test
        @DisplayName("多身份注册和 update 同一逻辑：每个身份 reserve → 写行 → confirm，都能查到且可还原")
        void addMultipleIdentitiesFallsBackToReservePath() {
            ShardedId userId = authIdFactory.newUserId(PHONE);
            LoginAccount account = LoginAccount.open(userId, authIdFactory.newIdentityId(userId), PHONE);
            account.bindIdentity(authIdFactory.newIdentityId(userId), EMAIL);

            repository.save(account);

            verify(lookupGateway, times(2)).reserve(any(), any());
            verify(lookupGateway, never()).insertConfirmed(any(), any());
            assertThat(account.hasChanges()).isFalse();
            assertThat(repository.findUserIdByIdentity(PHONE)).contains(userId);
            assertThat(repository.findUserIdByIdentity(EMAIL)).contains(userId);
            loadAndAssertLoginAccount(account);
        }

        @Test
        @DisplayName("（快速路径）手机号已被其他用户占用（lookup 唯一键冲突）时，抛 IdentifierAlreadyTakenException，账号不落库")
        void addFailedWhenIdentifierAlreadyInLookup() {
            // 预置冲突：同一标识符已存在于 identity_lookup
            IdentityLookupPO existing = newIdentityLookupPO();
            identityLookupMapper.insert(existing);

            LoginAccount account = newPhoneIdentityLoginAccount();

            assertThatThrownBy(() -> repository.save(account))
                    .isInstanceOf(IdentifierAlreadyTakenException.class);

            // 整个事务回滚，账号不应落库
            assertThat(repository.findByUserId(account.getUserId())).isEmpty();
        }

        @Test
        @DisplayName("（快速路径）user_identity 唯一键冲突时，抛 IdentifierAlreadyTakenException，账号不落库")
        void addFailedWhenIdentityAlreadyExists() {
            // 预置冲突：只有 identity 行、没有对应 lookup（绕过 lookup 的拦截，直接撞 identity 唯一键）
            UserIdentityPO existing = newUserIdentityPO(authIdFactory.newUserId(PHONE));
            identityMapper.insert(existing);

            LoginAccount account = newPhoneIdentityLoginAccount();

            assertThatThrownBy(() -> repository.save(account))
                    .isInstanceOf(IdentifierAlreadyTakenException.class);

            // 整个事务回滚，包括 lookup 已写入的部分
            assertThat(repository.findByUserId(account.getUserId())).isEmpty();
        }

        @Test
        @DisplayName("（快速路径）写入时出现非唯一键冲突的数据库异常（如超时），原样抛出，不被误译为已占用，且整体回滚")
        void addFailedWhenUnexpectedDataAccessException() {
            LoginAccount account = newPhoneIdentityLoginAccount();
            ShardedId userId = account.getUserId();
            RuntimeException timeout = new QueryTimeoutException("db timeout");

            // 在 lookup 写入处注入故障，模拟基础设施异常（真实环境无法稳定复现）
            doThrow(timeout).when(identityLookupMapper).insert(any(IdentityLookupPO.class));

            assertThatThrownBy(() -> repository.save(account))
                    .isNotInstanceOf(IdentifierAlreadyTakenException.class)
                    .isSameAs(timeout);

            // 事务回滚，账号不应落库
            assertThat(repository.findByUserId(userId)).isEmpty();
        }
    }

    @Nested
    @DisplayName("3. Update 更新（先 add 落库，再加载、修改、update）")
    class Update{

        @Test
        @DisplayName("增量新增：已有账号追加身份和因子，新身份进入 lookup，整体可还原，变更集清空")
        void update_addIdentityAndFactor() {
            ShardedId userId = persisted(newPhoneIdentityLoginAccount(false)).getUserId();

            LoginAccount loaded = load(userId);
            ShardedId emailId = authIdFactory.newIdentityId(userId);
            loaded.bindIdentity(emailId, EMAIL);
            loaded.verifyIdentity(emailId, now());
            UserMfaFactor factor = loaded.addIdentityFactor(authIdFactory.newFactorId(userId), emailId);
            loaded.enableFactor(factor.getId(), now());
            loaded.addSecretFactor(authIdFactory.newFactorId(userId), MfaFactorType.TOTP, "encrypted-secret");
            repository.save(loaded);

            assertThat(loaded.getVersion()).isEqualTo(1);
            assertThat(loaded.isNew()).isFalse();
            assertThat(loaded.hasChanges()).isFalse();

            assertThat(repository.findUserIdByIdentity(PHONE)).contains(userId);
            assertThat(repository.findUserIdByIdentity(EMAIL)).contains(userId);
            loadAndAssertLoginAccount(loaded);
        }

        /**
         * updateIdentity / updateFactor 是显式 set 的，漏写一列只有这里能发现。
         * 所以每个可变列都要改成和初始值（null / PENDING）不同的值，且各列用不同时间，避免串列。
         * 可变列：identity.verifiedAt / lastLoginAt；factor.status / verifiedAt / lastUsedAt。
         */
        @Test
        @DisplayName("修改所有可变列：身份 verifiedAt/lastLoginAt，因子 status/verifiedAt/lastUsedAt 都被写回")
        void update_allMutableColumns() {
            ShardedId userId = authIdFactory.newUserId(PHONE);
            ShardedId phoneId = authIdFactory.newIdentityId(userId);
            ShardedId factorId = authIdFactory.newFactorId(userId);
            LoginAccount account = LoginAccount.open(userId, phoneId, PHONE);
            account.addSecretFactor(factorId, MfaFactorType.TOTP, "enc");
            persisted(account);

            Instant base = now();
            LoginAccount loaded = load(userId);
            loaded.verifyIdentity(phoneId, base.plusSeconds(1));
            loaded.recordLogin(phoneId, base.plusSeconds(2));
            loaded.enableFactor(factorId, base.plusSeconds(3));
            loaded.recordFactorUsed(factorId, base.plusSeconds(4));
            repository.save(loaded);

            loadAndAssertLoginAccount(loaded);
        }

        @Test
        @DisplayName("删除身份和因子：对应行被删除，被删标识从 lookup 释放，保留的标识仍可查")
        void update_removeIdentityAndFactor() {
            ShardedId userId = authIdFactory.newUserId(PHONE);
            LoginAccount account = LoginAccount.open(userId, authIdFactory.newIdentityId(userId), PHONE);
            account.bindIdentity(authIdFactory.newIdentityId(userId), EMAIL);
            ShardedId factorId = authIdFactory.newFactorId(userId);
            account.addSecretFactor(factorId, MfaFactorType.TOTP, "enc");
            persisted(account);

            LoginAccount loaded = load(userId);
            ShardedId phoneId = loaded.getIdentities().stream()
                    .filter(i -> i.type().equals(PHONE.type())).findFirst().orElseThrow().getId();
            loaded.unbindIdentity(phoneId);
            loaded.removeFactor(factorId);
            repository.save(loaded);

            // 直接查表，不经过 findByUserId（它在没有身份行时不看因子，会掩盖因子行没删的问题）
            assertThat(identityMapper.selectByUserId(userId.value())).hasSize(1);
            assertThat(factorMapper.selectByUserId(userId.value())).isEmpty();
            assertThat(repository.findUserIdByIdentity(PHONE)).isEmpty();
            assertThat(repository.findUserIdByIdentity(EMAIL)).contains(userId);
            loadAndAssertLoginAccount(loaded);
        }

        @Test
        @DisplayName("注销账号：身份行、因子行全部删除，lookup 中的标识也被释放")
        void update_closeAccount() {
            ShardedId userId = authIdFactory.newUserId(PHONE);
            LoginAccount account = LoginAccount.open(userId, authIdFactory.newIdentityId(userId), PHONE);
            account.bindIdentity(authIdFactory.newIdentityId(userId), EMAIL);
            account.addSecretFactor(authIdFactory.newFactorId(userId), MfaFactorType.TOTP, "enc");
            persisted(account);

            LoginAccount loaded = load(userId);
            loaded.close();
            repository.save(loaded);
            assertThat(repository.findByUserId(userId)).isEmpty();
            assertThat(loginAccountMapper.selectByUserId(userId.value())).isNull();
            assertThat(identityMapper.selectByUserId(userId.value())).isEmpty();
            assertThat(factorMapper.selectByUserId(userId.value())).isEmpty();
            assertThat(repository.findUserIdByIdentity(PHONE)).isEmpty();
            assertThat(repository.findUserIdByIdentity(EMAIL)).isEmpty();
        }

        @Test
        @DisplayName("没有变更时 update 不碰数据库（identity / factor / lookup 都没有任何调用）")
        void update_noChanges_doesNotTouchDb() {
            ShardedId userId = persisted(newPhoneIdentityLoginAccount(true)).getUserId();
            LoginAccount loaded = load(userId);
            assertThat(loaded.hasChanges()).isFalse();
            clearInvocations(identityMapper, factorMapper, identityLookupMapper);

            repository.save(loaded);

            verifyNoInteractions(identityMapper, factorMapper, identityLookupMapper);
        }

        @Test
        @DisplayName("已有账号绑定被他人占用的标识：抛 IdentifierNotOwnedByUserException，双方数据和 lookup 都不变")
        void update_bindIdentifierTakenByOthers() {
            // 账号 A 占有 PHONE
            ShardedId userA = persisted(newPhoneIdentityLoginAccount(false)).getUserId();

            // 账号 B 只有 EMAIL
            ShardedId userB = authIdFactory.newUserId(EMAIL);
            persisted(LoginAccount.open(userB, authIdFactory.newIdentityId(userB), EMAIL));

            LoginAccount b = load(userB);
            b.bindIdentity(authIdFactory.newIdentityId(userB), PHONE);

            // update 走 lookup.reserve，冲突时会查出原因（为了幂等），所以抛的是细化后的异常：
            // 被他人持有 → IdentifierNotOwnedByUserException（IdentifierAlreadyTakenException 的子类）。
            // add 单身份走 insertConfirmed，唯一键冲突直接抛 IdentifierAlreadyTakenException，不查原因。
            assertThatThrownBy(() -> repository.save(b))
                    .isInstanceOf(IdentifierNotOwnedByUserException.class);

            // 失败不能误释放 A 的占位，也不能污染 B
            assertThat(repository.findUserIdByIdentity(PHONE)).contains(userA);
            assertThat(repository.findUserIdByIdentity(EMAIL)).contains(userB);
            assertThat(load(userB).getIdentities()).hasSize(1);
        }

        @Test
        @DisplayName("标识被释放后可复用：A 解绑手机号后，B 能用同一手机号绑定")
        void update_releasedIdentifierCanBeReused() {
            ShardedId userA = authIdFactory.newUserId(PHONE);
            ShardedId phoneIdA = authIdFactory.newIdentityId(userA);
            LoginAccount a = LoginAccount.open(userA, phoneIdA, PHONE);
            a.bindIdentity(authIdFactory.newIdentityId(userA), EMAIL);
            persisted(a);

            LoginAccount loadedA = load(userA);
            loadedA.unbindIdentity(phoneIdA);
            repository.save(loadedA);

            ShardedId userB = persisted(newPhoneIdentityLoginAccount(false)).getUserId();
            assertThat(repository.findUserIdByIdentity(PHONE)).contains(userB);
        }

        @Test
        @DisplayName("reserve 写入超时：异常原样抛出，变更集保留，聚合没有新增行")
        void reserveFailure() {
            ShardedId userId = persisted(newPhoneIdentityLoginAccount()).getUserId();
            LoginAccount updateAccount = load(userId);
            updateAccount.bindIdentity(authIdFactory.newIdentityId(userId), EMAIL);

            RuntimeException timeout = new QueryTimeoutException("db timeout");
            doThrow(timeout).when(identityLookupMapper).insertOrTouch(anyString(), anyString(), anyLong(), anyInt());

            assertThatThrownBy(() -> repository.save(updateAccount)).isSameAs(timeout);

            assertThat(updateAccount.hasChanges()).isTrue();
            assertThat(identityMapper.selectByUserId(userId.value())).hasSize(1);
        }
        @Test
        @DisplayName("reserve 抢占成功，但聚合插入失败；重试同一个账号对象成功")
        void bindIdentityInsertFailed(){
            ShardedId userId = persisted(newPhoneIdentityLoginAccount()).getUserId();

            LoginAccount updateAccount = load(userId);
            updateAccount.bindIdentity(authIdFactory.newIdentityId(userId), EMAIL);

            RuntimeException timeout = new QueryTimeoutException("db timeout");
            doThrow(timeout).when(identityMapper).insert(any(UserIdentityPO.class));
            clearInvocations(lookupGateway,identityMapper);

            assertThatThrownBy(()->repository.save(updateAccount)).isInstanceOf(QueryTimeoutException.class);
            assertThat(updateAccount.hasChanges()).isTrue();
            assertThat(updateAccount.addedIdentities()).isNotEmpty();

            assertThat(lookupGateway.findUserId(EMAIL)).isEmpty();
            verify(lookupGateway).reserve(EMAIL, userId);
            verify(lookupGateway).cancelReserve(EMAIL, userId);
            assertThat(identityMapper.selectByUserId(userId.value())).hasSize(1);

            // 重试
            reset(identityMapper);
            repository.save(updateAccount);
            assertThat(updateAccount.hasChanges()).isFalse();
            assertThat(repository.findUserIdByIdentity(EMAIL)).contains(userId);
            loadAndAssertLoginAccount(updateAccount);
        }

        @Test
        @DisplayName("写入成功后 confirm 失败：update 不抛异常，lookup 停在 RESERVED，等待回收任务")
        void confirmFailureAfterCommit() {
            ShardedId userId = persisted(newPhoneIdentityLoginAccount()).getUserId();
            LoginAccount account = load(userId);
            account.bindIdentity(authIdFactory.newIdentityId(userId), EMAIL);

            doThrow(new QueryTimeoutException("db timeout"))
                    .when(lookupGateway).confirm(EMAIL, userId);

            assertThatCode(() -> repository.save(account)).doesNotThrowAnyException();

            assertThat(account.hasChanges()).isFalse();
            assertThat(repository.findUserIdByIdentity(EMAIL)).isEmpty();           // RESERVED 查不到
            assertThat(identityMapper.selectByUserId(userId.value())).hasSize(2);   // 数据已提交
            assertThat(repository.findUserIdByIdentity(PHONE)).isNotEmpty();           // RESERVED 查不到
        }

        @Test
        @DisplayName("解绑时聚合删除失败：abortRelease 恢复，标识仍可路由，重试成功")
        void unbindIdentityDeleteFailed() {
            ShardedId userId = authIdFactory.newUserId(PHONE);
            ShardedId phoneId = authIdFactory.newIdentityId(userId);
            LoginAccount account = LoginAccount.open(userId, phoneId, PHONE);
            account.bindIdentity(authIdFactory.newIdentityId(userId), EMAIL);
            persisted(account);

            LoginAccount loaded = load(userId);
            loaded.unbindIdentity(phoneId);

            doThrow(new QueryTimeoutException("db timeout"))
                    .when(identityMapper).delete(any(Wrapper.class));   // com.baomidou.mybatisplus.core.conditions.Wrapper

            assertThatThrownBy(() -> repository.save(loaded)).isInstanceOf(QueryTimeoutException.class);
            assertThat(loaded.hasChanges()).isTrue();
            assertThat(identityMapper.selectByUserId(userId.value())).hasSize(2);
            assertThat(repository.findUserIdByIdentity(PHONE)).contains(userId);   // abortRelease 已恢复

            reset(identityMapper);
            repository.save(loaded);   // 重试

            assertThat(loaded.hasChanges()).isFalse();
            assertThat(repository.findUserIdByIdentity(PHONE)).isEmpty();
            assertThat(repository.findUserIdByIdentity(EMAIL)).contains(userId);
        }

        @Test
        @DisplayName("解绑后 completeRelease 失败：update 不抛异常，身份行已删，等待回收任务")
        void completeReleaseFailureAfterCommit() {
            ShardedId userId = authIdFactory.newUserId(PHONE);
            ShardedId phoneId = authIdFactory.newIdentityId(userId);
            LoginAccount account = LoginAccount.open(userId, phoneId, PHONE);
            account.bindIdentity(authIdFactory.newIdentityId(userId), EMAIL);
            persisted(account);

            LoginAccount loaded = load(userId);
            loaded.unbindIdentity(phoneId);
            doThrow(new QueryTimeoutException("db timeout"))
                    .when(lookupGateway).completeRelease(PHONE, userId);

            assertThatCode(() -> repository.save(loaded)).doesNotThrowAnyException();

            assertThat(loaded.hasChanges()).isFalse();
            assertThat(repository.findUserIdByIdentity(PHONE)).isEmpty();
            assertThat(identityMapper.selectByUserId(userId.value())).hasSize(1);   // 数据已提交
            assertThat(repository.findUserIdByIdentity(EMAIL)).contains(userId);
        }
    }
    @RepeatedTest(5)
    @DisplayName("并发：两个账号同时绑定同一个手机号，只有一个成功，输家回滚且不影响赢家")
    void concurrentBindSameIdentifier_onlyOneWins() throws Exception {
        ShardedId userA = authIdFactory.newUserId(EmailIdentifier.from("a@example.com"));
        persisted(LoginAccount.open(userA, authIdFactory.newIdentityId(userA), EmailIdentifier.from("a@example.com")));
        ShardedId userB = authIdFactory.newUserId(EmailIdentifier.from("b@example.com"));
        persisted(LoginAccount.open(userB, authIdFactory.newIdentityId(userB), EmailIdentifier.from("b@example.com")));

        LoginAccount a = load(userA);
        a.bindIdentity(authIdFactory.newIdentityId(userA), PHONE);
        LoginAccount b = load(userB);
        b.bindIdentity(authIdFactory.newIdentityId(userB), PHONE);

        int success = saveWithThreadPool(a, b, IdentifierNotOwnedByUserException.class);

        assertThat(success).isEqualTo(1);
        ShardedId winner = repository.findUserIdByIdentity(PHONE).orElseThrow();
        ShardedId loser = winner.equals(userA) ? userB : userA;
        assertThat(load(winner).getIdentities()).hasSize(2);
        assertThat(load(loser).getIdentities()).hasSize(1);   // 输家回滚
    }

    @RepeatedTest(5)
    @DisplayName("并发：一个账号并发移除不同的身份")
    void concurrentUnbindDifferentIdentifier_onlyOneWins() throws Exception {
        LoginAccount newed = newPhoneIdentityLoginAccount(false);
        ShardedId userId = newed.getUserId();
        newed.bindIdentity(authIdFactory.newIdentityId(userId), EMAIL);
        persisted(newed);
        long version = newed.getVersion();
        LoginAccount a0 = load(userId);
        a0.unbindIdentity(a0.getIdentities().getFirst().getId());

        LoginAccount a1 = load(userId);
        a1.unbindIdentity(a1.getIdentities().get(1).getId());


        int success = saveWithThreadPool(a0, a1, AccountStatusChangedException.class);

        assertThat(success).isEqualTo(1);

        LoginAccount updated = load(userId);
        assertThat(updated.getVersion()).isEqualTo(version+1);
        assertThat(updated.getIdentities()).hasSize(1);
    }

    private int saveWithThreadPool(LoginAccount a0, LoginAccount a1, Class<?> expectedException) throws InterruptedException, TimeoutException {
        CyclicBarrier barrier = new CyclicBarrier(2);
        int success;
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            List<Future<?>> futures = List.of(
                    pool.submit(() -> {
                        barrier.await();
                        repository.save(a0);
                        return null;
                    }),
                    pool.submit(() -> {
                        barrier.await();
                        repository.save(a1);
                        return null;
                    }));

            success = 0;
            for (Future<?> f : futures) {
                try {
                    f.get(10, TimeUnit.SECONDS);
                    success++;
                } catch (ExecutionException e) {
                    assertThat(e.getCause()).isInstanceOf(expectedException); // 含子类
                }
            }
            pool.shutdown();
        }
        return success;
    }

    // --------------------------------- private
    private LoginAccount newPhoneIdentityLoginAccount(){
        return newPhoneIdentityLoginAccount(true);
    }
    private LoginAccount newPhoneIdentityLoginAccount(boolean addFactor){
        ShardedId userId = authIdFactory.newUserId(PHONE);
        ShardedId identityId = authIdFactory.newIdentityId(userId);

        LoginAccount account = LoginAccount.open(userId, identityId, PHONE);
        if (addFactor){
            account.verifyIdentity(identityId,  now());
            ShardedId factorId = authIdFactory.newFactorId(userId);
            account.addIdentityFactor(factorId, identityId);
        }
        return account;
    }

    /** 走生产路径（add）把账号落库，作为 Update 用例的初始状态 */
    private LoginAccount persisted(LoginAccount account) {
        repository.save(account);
        return account;
    }

    private LoginAccount load(ShardedId userId) {
        return repository.findByUserId(userId).orElseThrow();
    }


    private IdentityLookupPO newIdentityLookupPO(){
        IdentityLookupPO po = new IdentityLookupPO();
        po.setUserId(newId("user", 1).value());
        po.setIdentityType(PHONE.type().getCode());
        po.setIdentifier(PHONE.value());
        return po;
    }

    private LoginAccountPO newLoginAccountPO(ShardedId userId) {
        LoginAccountPO accountPO = new LoginAccountPO();
        accountPO.setUserId(userId.value());
        return accountPO;
    }

    private UserIdentityPO newUserIdentityPO(ShardedId userId){
        UserIdentityPO ui = new UserIdentityPO();
        ui.setUserId(userId.value());
        ui.setId(authIdFactory.newIdentityId(userId).value());
        ui.setIdentityType(PHONE.type().getCode());
        ui.setIdentifier(PHONE.value());
        return ui;
    }

    private UserMfaFactorPO newUserMfaFactorPO(ShardedId userId){
        UserMfaFactorPO po = new UserMfaFactorPO();
        po.setFactorType(MfaFactorType.SMS.getCode());
        po.setUserId(userId.value());
        po.setId(authIdFactory.newFactorId(userId).value());
        po.setSecretEncrypted("test secretEncrypted");
        return po;
    }

    /** 按 id 排序后逐项比较，不依赖 selectByUserId 的返回顺序 */
    private static <T> List<T> sortedById(List<T> list, Function<T, ShardedId> idOf) {
        return list.stream()
                .sorted(Comparator.comparingLong((T t) -> idOf.apply(t).value()))
                .toList();
    }

    private void loadAndAssertLoginAccount(LoginAccount account){

        Optional<LoginAccount> loaded = repository.findByUserId(account.getUserId());

        assertThat(loaded).isPresent();
        LoginAccount la = loaded.get();
        assertThat(la.getUserId()).isEqualTo(account.getUserId());
        assertThat(la.isNew()).isEqualTo(account.isNew());
        assertThat(la.getVersion()).isEqualTo(account.getVersion());

        List<UserIdentity> expectedIdentities = sortedById(account.getIdentities(), UserIdentity::getId);
        List<UserIdentity> actualIdentities = sortedById(la.getIdentities(), UserIdentity::getId);
        assertThat(actualIdentities).hasSize(expectedIdentities.size());
        for (int i = 0; i < expectedIdentities.size(); i++) {
            UserIdentity identity = expectedIdentities.get(i);
            UserIdentity laIdentity = actualIdentities.get(i);
            assertThat(laIdentity.getId()).isEqualTo(identity.getId());
            assertThat(laIdentity.type()).isEqualTo(identity.type());
            assertThat(laIdentity.getIdentifier()).isEqualTo(identity.getIdentifier());
            assertThat(laIdentity.getVerifiedAt()).isEqualTo(identity.getVerifiedAt());
            assertThat(laIdentity.getLastLoginAt()).isEqualTo(identity.getLastLoginAt());
        }

        List<UserMfaFactor> expectedFactors = sortedById(account.getFactors(), UserMfaFactor::getId);
        List<UserMfaFactor> actualFactors = sortedById(la.getFactors(), UserMfaFactor::getId);
        assertThat(actualFactors).hasSize(expectedFactors.size());
        for (int i = 0; i < expectedFactors.size(); i++) {
            UserMfaFactor factor = expectedFactors.get(i);
            UserMfaFactor laFactor = actualFactors.get(i);

            assertThat(laFactor.getId()).isEqualTo(factor.getId());
            assertThat(laFactor.getType()).isEqualTo(factor.getType());
            assertThat(laFactor.getIdentityId()).isEqualTo(factor.getIdentityId());
            assertThat(laFactor.getSecretEncrypted()).isEqualTo(factor.getSecretEncrypted());
            assertThat(laFactor.getStatus()).isEqualTo(factor.getStatus());
            assertThat(laFactor.getVerifiedAt()).isEqualTo(factor.getVerifiedAt());
            assertThat(laFactor.getLastUsedAt()).isEqualTo(factor.getLastUsedAt());
        }

    }

}