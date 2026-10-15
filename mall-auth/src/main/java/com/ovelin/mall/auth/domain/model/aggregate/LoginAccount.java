package com.ovelin.mall.auth.domain.model.aggregate;

import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import com.ovelin.mall.auth.domain.model.enums.MfaFactorType;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifier;
import com.ovelin.mall.common.exception.global.InvalidArgumentException;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
 * 变更跟踪：聚合内部记录新增/修改/删除的子实体，仓储 save 后调用 {@link #clearChanges()}。
 * 子实体的构造与修改方法均为包级可见，外部只能通过本聚合根操作。
 */
public class LoginAccount {

    @Getter
    private final ShardedId userId;
    private final List<UserIdentity> identities = new ArrayList<>();
    private final List<MfaFactor> factors = new ArrayList<>();

    private final ChangeSet<UserIdentity> identityChanges = new ChangeSet<>();
    private final ChangeSet<MfaFactor> factorChanges = new ChangeSet<>();

    private LoginAccount(ShardedId userId) {
        this.userId = userId;
    }

    // ------------------------------------------------------------------ 创建 / 还原

    /** 注册：创建账号并带上首个登录身份 */
    public static LoginAccount open(ShardedId userId, ShardedId identityId,
                                    IdentityIdentifier identityIdentifier) {
        LoginAccount account = new LoginAccount(userId);
        UserIdentity identity = UserIdentity.register(identityId, userId, identityIdentifier);
        account.identities.add(identity);
        account.identityChanges.added(identity.getId(), identity);
        return account;
    }

    /** 仅供持久化层还原：不校验、不产生变更记录 */
    public static LoginAccount reconstitute(ShardedId userId, List<UserIdentity> identities,
                                            List<MfaFactor> factors) {
        LoginAccount account = new LoginAccount(userId);
        account.identities.addAll(identities);
        account.factors.addAll(factors);
        return account;
    }

    // ------------------------------------------------------------------ 身份

    /** 新增登录身份（未验证） */
    public UserIdentity bindIdentity(ShardedId identityId, IdentityIdentifier identityIdentifier) {
        UserIdentity identity = UserIdentity.register(identityId, userId, identityIdentifier);
        if (findIdentity(identityIdentifier).isPresent()) {
            throw new IllegalStateException("该登录标识已绑定到当前账号");
        }
        identities.add(identity);
        identityChanges.added(identity.getId(), identity);
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
            throw new IllegalStateException("至少保留一个登录身份");
        }
        for (MfaFactor factor : factors) {
            if (factor.references(identityId) && factor.disable()) {
                factorChanges.dirty(factor.getId(), factor);
            }
        }
        identities.remove(target);
        identityChanges.removed(target.getId(), target);
    }

    // ------------------------------------------------------------------ 二次验证因子

    /** 短信/邮箱因子：引用本账号内一个已验证的手机号/邮箱身份 */
    public MfaFactor addIdentityFactor(ShardedId factorId, ShardedId identityId) {
        UserIdentity identity = requireIdentity(identityId);
        boolean exists = factors.stream()
                .anyMatch(f -> f.references(identityId) && !f.isDisabled());
        if (exists) {
            throw new IllegalStateException("该身份已存在未停用的二次验证因子");
        }
        MfaFactor factor = MfaFactor.forIdentity(factorId, identity);
        factors.add(factor);
        factorChanges.added(factor.getId(), factor);
        return factor;
    }

    /** TOTP / WebAuthn / 恢复码：secretEncrypted 由应用服务加密后传入 */
    public MfaFactor addSecretFactor(ShardedId factorId, MfaFactorType type, String secretEncrypted) {
        MfaFactor factor = MfaFactor.withSecret(factorId, userId, type, secretEncrypted);
        factors.add(factor);
        factorChanges.added(factor.getId(), factor);
        return factor;
    }

    public void enableFactor(ShardedId factorId, Instant at) {
        MfaFactor factor = requireFactor(factorId);
        if (factor.getIdentityId() != null) {
            // 引用身份的因子：启用时身份必须仍然存在且已验证
            UserIdentity identity = requireIdentity(factor.getIdentityId());
            if (!identity.canReceiveCode()) {
                throw new IllegalStateException("该身份不能用于二次验证：必须是已验证的手机号或邮箱");
            }
        }
        if (factor.enable(at)) {
            factorChanges.dirty(factor.getId(), factor);
        }
    }

    public void disableFactor(ShardedId factorId) {
        MfaFactor factor = requireFactor(factorId);
        if (factor.disable()) {
            factorChanges.dirty(factor.getId(), factor);
        }
    }

    public void recordFactorUsed(ShardedId factorId, Instant at) {
        MfaFactor factor = requireFactor(factorId);
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
        for (MfaFactor factor : List.copyOf(factors)) {
            removeFactorInternal(factor);
        }
        for (UserIdentity identity : List.copyOf(identities)) {
            identities.remove(identity);
            identityChanges.removed(identity.getId(), identity);
        }
    }

    // ------------------------------------------------------------------ 查询

    public List<UserIdentity> getIdentities() {
        return Collections.unmodifiableList(identities);
    }

    public List<MfaFactor> getFactors() {
        return Collections.unmodifiableList(factors);
    }

    public Optional<UserIdentity> findIdentity(ShardedId identityId) {
        return identities.stream().filter(i -> i.getId().equals(identityId)).findFirst();
    }

    public Optional<UserIdentity> findIdentity(IdentityIdentifier identifier) {
        return identities.stream().filter(i -> i.matches(identifier)).findFirst();
    }

    public List<MfaFactor> enabledFactors() {
        return factors.stream().filter(MfaFactor::isEnabled).toList();
    }

    /** 是否需要二次验证：只统计状态为启用的因子 */
    public boolean requiresMfa() {
        return factors.stream().anyMatch(MfaFactor::isEnabled);
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

    public List<MfaFactor> addedFactors() {
        return factorChanges.addedList();
    }

    public List<MfaFactor> dirtyFactors() {
        return factorChanges.dirtyList();
    }

    public List<MfaFactor> removedFactors() {
        return factorChanges.removedList();
    }

    public boolean hasChanges() {
        return !identityChanges.isEmpty() || !factorChanges.isEmpty();
    }

    /** 仓储 save 成功后调用 */
    public void clearChanges() {
        identityChanges.clear();
        factorChanges.clear();
    }

    // ------------------------------------------------------------------ 内部

    private UserIdentity requireIdentity(ShardedId identityId) {
        return findIdentity(identityId)
                .orElseThrow(() -> new InvalidArgumentException("登录身份不存在"));
    }

    private MfaFactor requireFactor(ShardedId factorId) {
        return factors.stream().filter(f -> f.getId().equals(factorId)).findFirst()
                .orElseThrow(() -> new InvalidArgumentException("二次验证因子不存在"));
    }

    private void removeFactorInternal(MfaFactor factor) {
        factors.remove(factor);
        factorChanges.removed(factor.getId(), factor);
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