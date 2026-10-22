package com.ovelin.mall.auth.domain.model.aggregate;

import com.ovelin.mall.auth.domain.exception.IdentifierAlreadyBoundException;
import com.ovelin.mall.auth.domain.exception.IdentityNotExistException;
import com.ovelin.mall.auth.domain.exception.MfaFactorNotExistException;
import com.ovelin.mall.auth.domain.model.enums.LoginAccountStatus;
import com.ovelin.mall.auth.domain.model.enums.MfaFactorType;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifier;
import com.ovelin.mall.common.exception.global.InvalidArgumentException;
import com.ovelin.mall.common.exception.global.InvalidStatusException;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import lombok.Getter;

import java.time.Instant;
import java.util.*;

/**
 * 登录账号聚合根：持有同一用户的登录身份（identities）与二次验证因子（mfaFactors）。
 * <p>
 * 一致性边界：
 * <ul>
 *   <li>至少保留一个登录身份（close() 注销除外）</li>
 *   <li>同一账号内 (type, identifier) 不重复</li>
 *   <li>解绑身份时，先停用引用该身份的 sms/email 因子，再移除身份</li>
 *   <li>只有已验证的手机号/邮箱才能创建、启用 sms/email 因子</li>
 * </ul>
 * 密码（UserPassword）不在本聚合内：它只有自己的规则，且登录失败计数是高频写，单独成聚合避免写冲突。
 * <p>
 * 变更跟踪：聚合内部记录新增/修改/删除的子实体，仓储 save 后调用 {@link #makeSaved()}。
 * 子实体的构造与修改方法均为包级可见，外部只能通过本聚合根操作。
 */
public class LoginAccount {

    @Getter
    private final ShardedId userId;
    @Getter
    private long version;

    private LoginAccountStatus status;
    private final List<UserIdentity> identities = new ArrayList<>();
    private final List<UserMfaFactor> factors = new ArrayList<>();

    private final ChangeSet<UserIdentity> identityChanges = new ChangeSet<>();
    private final ChangeSet<UserMfaFactor> factorChanges = new ChangeSet<>();

    private LoginAccount(ShardedId userId, long version, boolean isNew) {
        this.userId = userId;
        this.version = version;
        if (isNew) this.status = LoginAccountStatus.CREATING;
    }

    // ------------------------------------------------------------------ 创建 / 还原

    /** 注册：创建账号并带上首个登录身份 */
    public static LoginAccount open(ShardedId userId, ShardedId identityId,
                                    IdentityIdentifier identityIdentifier) {
        LoginAccount account = new LoginAccount(userId, 0, true);
        account.bindIdentity(identityId, identityIdentifier);
        return account;
    }


    /** 仅供持久化层还原：不校验、不产生变更记录 */
    public static LoginAccount reconstitute(ShardedId userId, long version, List<UserIdentity> identities,
                                            List<UserMfaFactor> factors) {
        LoginAccount account = new LoginAccount(userId, version, false);
        account.identities.addAll(identities);
        account.factors.addAll(factors);
        return account;
    }
    public boolean isNew(){
        return status == LoginAccountStatus.CREATING;
    }
    public boolean isClose(){
        return status == LoginAccountStatus.CLOSING;
    }
    public boolean isClosed(){
        return status == LoginAccountStatus.CLOSED;
    }

    /**
     * 该操作会把LoginAccount 从isNew和isClosing中脱离出来，并且会删掉变化，使得LoginAccount的状态和数据库一致
     * <p>注意：并不会回滚对内存中LoginAccount的修改</p>
     */
    public void makeSaved(){
        if (status == LoginAccountStatus.CREATING){
            status = LoginAccountStatus.ACTIVE;
        } else if (status == LoginAccountStatus.CLOSING){
            status = LoginAccountStatus.CLOSED;
        } else {
            version ++;
        }

        if (this.hasChanges()){
            this.clearChanges();
        }
    }

    // ------------------------------------------------------------------ 身份

    /** 新增登录身份（未验证） */
    public UserIdentity bindIdentity(ShardedId identityId, IdentityIdentifier identityIdentifier) {
        UserIdentity identity = UserIdentity.register(identityId, userId, identityIdentifier);
        if (findIdentity(identityIdentifier).isPresent()) {
            throw new IdentifierAlreadyBoundException(identityIdentifier);
        }
        addIdentityInternal(identity);
        return identity;
    }

    public void verifyIdentity(ShardedId identityId, Instant at) {
        UserIdentity identity = requireIdentity(identityId);
        if (identity.markVerified(at)) {
            identityChanges.dirty(identity.getId(), identity);
        }
    }

    public void recordLogin(ShardedId identityId, Instant at) {
        UserIdentity identity = requireIdentity(identityId);
        identity.recordLogin(at);
        identityChanges.dirty(identity.getId(), identity);
    }

    /**
     * 解绑身份。内部顺序固定：
     * 1) 停用引用该身份的 sms/email 因子（防止"因子启用但验证码发不出"）
     * 2) 移除身份
     * 仓储落库时需保证：因子的更新先于身份的删除。
     */
    public void unbindIdentity(ShardedId identityId) {
        UserIdentity target = requireIdentity(identityId);
        if (identities.size() <= 1) {
            throw new InvalidArgumentException("至少保留一个登录身份");
        }
        for (Iterator<UserMfaFactor> it = factors.iterator(); it.hasNext(); ) {
            UserMfaFactor factor = it.next();
            if (factor.references(identityId)) {
                it.remove();
                factorChanges.removed(factor.getId(), factor);
            }
        }
        removeIdentityInternal(target);
    }

    // ------------------------------------------------------------------ 二次验证因子

    /** 短信/邮箱因子：引用本账号内一个已验证的手机号/邮箱身份 */
    public UserMfaFactor addIdentityFactor(ShardedId factorId, ShardedId identityId) {
        UserIdentity identity = requireIdentity(identityId);
        boolean exists = factors.stream()
                .anyMatch(f -> f.references(identityId) && !f.isDisabled());
        if (exists) {
            throw new InvalidStatusException("该身份已存在未停用的二次验证因子");
        }
        UserMfaFactor factor = UserMfaFactor.forIdentity(factorId, identity);

        addFactorInternal(factor);
        return factor;
    }


    /** TOTP / WebAuthn / 恢复码：secretEncrypted 由应用服务加密后传入 */
    public UserMfaFactor addSecretFactor(ShardedId factorId, MfaFactorType type, String secretEncrypted) {
        UserMfaFactor factor = UserMfaFactor.withSecret(factorId, userId, type, secretEncrypted);
        addFactorInternal(factor);
        return factor;
    }

    public void enableFactor(ShardedId factorId, Instant at) {
        UserMfaFactor factor = requireFactor(factorId);
        if (factor.getIdentityId() != null) {
            // 引用身份的因子：启用时身份必须仍然存在且已验证
            UserIdentity identity = requireIdentity(factor.getIdentityId());
            if (!identity.canReceiveCode()) {
                throw new InvalidStatusException("该身份不能用于二次验证：必须是已验证的手机号或邮箱");
            }
        }
        if (factor.enable(at)) {
            factorChanges.dirty(factor.getId(), factor);
        }
    }

    public void disableFactor(ShardedId factorId) {
        UserMfaFactor factor = requireFactor(factorId);
        if (factor.disable()) {
            factorChanges.dirty(factor.getId(), factor);
        }
    }

    public void recordFactorUsed(ShardedId factorId, Instant at) {
        UserMfaFactor factor = requireFactor(factorId);
        factor.markUsed(at);
        factorChanges.dirty(factor.getId(), factor);
    }

    public void removeFactor(ShardedId factorId) {
        removeFactorInternal(requireFactor(factorId));
    }

    // ------------------------------------------------------------------ 注销

    /**
     * 注销：移除全部因子与身份（不受"至少一个身份"约束）。
     * 仓储落库顺序：先因子、后身份；身份删除前先释放 lookup，保证失败可重试收敛。
     */
    public void close() {
        this.status = LoginAccountStatus.CLOSING;
        for (UserMfaFactor factor : List.copyOf(factors)) {
            removeFactorInternal(factor);
        }
        for (UserIdentity identity : List.copyOf(identities)) {
            removeIdentityInternal(identity);
        }
    }

    // ------------------------------------------------------------------ 查询

    public List<UserIdentity> getIdentities() {
        return Collections.unmodifiableList(identities);
    }

    public List<UserMfaFactor> getFactors() {
        return Collections.unmodifiableList(factors);
    }

    public Optional<UserIdentity> findIdentity(ShardedId identityId) {
        return identities.stream().filter(i -> i.getId().equals(identityId)).findFirst();
    }

    public Optional<UserIdentity> findIdentity(IdentityIdentifier identifier) {
        return identities.stream().filter(i -> i.matches(identifier)).findFirst();
    }

    public List<UserMfaFactor> enabledFactors() {
        return factors.stream().filter(UserMfaFactor::isEnabled).toList();
    }

    /** 是否需要二次验证：只统计状态为启用的因子 */
    public boolean requiresMfa() {
        return factors.stream().anyMatch(UserMfaFactor::isEnabled);
    }

    // ------------------------------------------------------------------ 变更集（供仓储使用）

    public List<UserIdentity> addedIdentities() {
        return identityChanges.addedList();
    }

    public List<UserIdentity> dirtyIdentities() {
        return identityChanges.dirtyList();
    }

    public List<UserIdentity> removedIdentities() {
        return identityChanges.removedList();
    }

    public List<UserMfaFactor> addedFactors() {
        return factorChanges.addedList();
    }

    public List<UserMfaFactor> dirtyFactors() {
        return factorChanges.dirtyList();
    }

    public List<UserMfaFactor> removedFactors() {
        return factorChanges.removedList();
    }

    public boolean hasChanges() {
        return !identityChanges.isEmpty() || !factorChanges.isEmpty();
    }

    /** 仓储 add/update 成功后调用 */
    private void clearChanges() {
        identityChanges.clear();
        factorChanges.clear();
    }

    // ------------------------------------------------------------------ 内部

    private UserIdentity requireIdentity(ShardedId identityId) {
        return findIdentity(identityId)
                .orElseThrow(() -> new IdentityNotExistException(identityId));
    }

    private UserMfaFactor requireFactor(ShardedId factorId) {
        return factors.stream().filter(f -> f.getId().equals(factorId)).findFirst()
                .orElseThrow(() -> new MfaFactorNotExistException(factorId));
    }

    private void addIdentityInternal(UserIdentity identity) {
        identities.add(identity);
        identityChanges.added(identity.getId(), identity);
    }

    private void addFactorInternal(UserMfaFactor factor) {
        factors.add(factor);
        factorChanges.added(factor.getId(), factor);
    }

    private void removeFactorInternal(UserMfaFactor factor) {
        factors.remove(factor);
        factorChanges.removed(factor.getId(), factor);
    }

    private void removeIdentityInternal(UserIdentity identity) {
        identities.remove(identity);
        identityChanges.removed(identity.getId(), identity);
    }

    /** 子实体变更集：新增的对象即使后续被修改也只算 added；新增后又删除则相互抵消 */
    private static final class ChangeSet<T> {
        private final Map<ShardedId, T> added = new LinkedHashMap<>();
        private final Map<ShardedId, T> dirty = new LinkedHashMap<>();
        private final Map<ShardedId, T> removed = new LinkedHashMap<>();

        void added(ShardedId id, T entity) {
            added.put(id, entity);
        }

        void dirty(ShardedId id, T entity) {
            if (!added.containsKey(id)) {
                dirty.put(id, entity);
            }
        }

        void removed(ShardedId id, T entity) {
            if (added.remove(id) != null) {
                return;
            }
            dirty.remove(id);
            removed.put(id, entity);
        }

        List<T> addedList() {
            return List.copyOf(added.values());
        }

        List<T> dirtyList() {
            return List.copyOf(dirty.values());
        }

        List<T> removedList() {
            return List.copyOf(removed.values());
        }

        boolean isEmpty() {
            return added.isEmpty() && dirty.isEmpty() && removed.isEmpty();
        }

        void clear() {
            added.clear();
            dirty.clear();
            removed.clear();
        }
    }
}