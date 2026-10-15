package com.ovelin.mall.auth.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ovelin.mall.auth.domain.model.aggregate.LoginAccount;
import com.ovelin.mall.auth.domain.model.aggregate.MfaFactor;
import com.ovelin.mall.auth.domain.model.aggregate.UserIdentity;
import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifier;
import com.ovelin.mall.auth.domain.repository.LoginAccountRepository;


import com.ovelin.mall.auth.infrastructure.persistence.convertor.MfaFactorPOConverter;
import com.ovelin.mall.auth.infrastructure.persistence.convertor.UserIdentityPOConverter;
import com.ovelin.mall.auth.infrastructure.persistence.gateway.IdentityLookupGateway;
import com.ovelin.mall.auth.infrastructure.persistence.mapper.UserIdentityMapper;

import com.ovelin.mall.auth.infrastructure.persistence.mapper.UserMfaFactorMapper;
import com.ovelin.mall.auth.infrastructure.persistence.po.UserIdentityPO;
import com.ovelin.mall.auth.infrastructure.persistence.po.UserMfaFactorPO;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 保存流程：
 * <ol>
 *   <li>对新增身份在 identity_lookup 占位（PENDING，独立提交）；冲突即抛 IdentifierAlreadyTakenException</li>
 *   <li>对被移除的身份先释放 lookup（身份行删除前先清，失败可重试收敛）</li>
 *   <li>同分片本地事务内写聚合各行（加入外层事务，没有则自己开启）</li>
 *   <li>事务提交后确认占位（ACTIVE）；回滚则释放占位。进程崩溃遗留的 PENDING 由回收任务兜底</li>
 * </ol>
 * 步骤 4 挂在事务同步回调上，而不是写在 save() 里：注册时 save() 可能处在外层事务中，
 * 提交失败发生在 save() 返回之后，直接在 save() 里补偿会漏掉这种情况。
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class LoginAccountRepositoryImpl implements LoginAccountRepository {

    private final UserIdentityMapper identityMapper;
    private final UserMfaFactorMapper factorMapper;
    private final IdentityLookupGateway lookup;
    private final TransactionTemplate tx;

    @Override
    public Optional<LoginAccount> findByUserId(ShardedId userId) {
        Long uid = toLong(userId);
        List<UserIdentity> identities = identityMapper.selectList(Wrappers.<UserIdentityPO>lambdaQuery()
                        .eq(UserIdentityPO::getUserId, uid))
                .stream().map(UserIdentityPOConverter::toDomain).toList();
        if (identities.isEmpty()) {
            return Optional.empty();
        }
        List<MfaFactor> factors = factorMapper.selectList(Wrappers.<UserMfaFactorPO>lambdaQuery()
                        .eq(UserMfaFactorPO::getUserId, uid))
                .stream().map(MfaFactorPOConverter::toDomain).toList();
        return Optional.of(LoginAccount.reconstitute(userId, identities, factors));
    }

    @Override
    public Optional<ShardedId> findUserIdByIdentity(IdentityIdentifier identityIdentifier) {
        return lookup.findUserId(identityIdentifier.type(), identityIdentifier.value());
    }

    @Override
    public void save(LoginAccount account) {
        if (!account.hasChanges()) {
            return;
        }
        ShardedId userId = account.getUserId();
        List<UserIdentity> reserved = new ArrayList<>();
        try {
            for (UserIdentity identity : account.addedIdentities()) {
                lookup.reserve(identity.getType(), identity.getIdentifier(), userId);
                reserved.add(identity);
            }
            for (UserIdentity identity : account.removedIdentities()) {
                lookup.release(identity.getType(), identity.getIdentifier(), userId);
            }
            tx.executeWithoutResult(status -> {
                registerLookupCallbacks(reserved, userId);
                writeRows(account);
            });
        } catch (RuntimeException e) {
            // 幂等：回滚回调也会释放，这里兜住"事务都没能开始"的情况
            reserved.forEach(i -> releaseQuietly(i, userId));
            throw e;
        }
        account.clearChanges();
    }

    // ------------------------------------------------------------------ 落库（同分片本地事务内）

    /** 顺序：因子删/改 → 身份删 → 身份改 → 身份增 → 因子增（因子引用的身份先于因子存在） */
    private void writeRows(LoginAccount account) {
        account.removedFactors().forEach(this::deleteFactor);
        account.dirtyFactors().forEach(this::updateFactor);
        account.removedIdentities().forEach(this::deleteIdentity);
        account.dirtyIdentities().forEach(this::updateIdentity);
        account.addedIdentities().forEach(i -> identityMapper.insert(UserIdentityPOConverter.toPO(i)));
        account.addedFactors().forEach(f -> factorMapper.insert(MfaFactorPOConverter.toPO(f)));
    }

    /** 显式 set（可写回 null）并带分片键 user_id；不可变列（类型、标识、引用、密文）不更新 */
    private void updateIdentity(UserIdentity identity) {
        UserIdentityPO po = UserIdentityPOConverter.toPO(identity);
        identityMapper.update(null, Wrappers.<UserIdentityPO>lambdaUpdate()
                .set(UserIdentityPO::getVerifiedAt, po.getVerifiedAt())
                .set(UserIdentityPO::getLastLoginAt, po.getLastLoginAt())
                .eq(UserIdentityPO::getId, po.getId())
                .eq(UserIdentityPO::getUserId, po.getUserId()));
    }

    private void updateFactor(MfaFactor factor) {
        UserMfaFactorPO po = MfaFactorPOConverter.toPO(factor);
        factorMapper.update(null, Wrappers.<UserMfaFactorPO>lambdaUpdate()
                .set(UserMfaFactorPO::getStatus, po.getStatus())
                .set(UserMfaFactorPO::getVerifiedAt, po.getVerifiedAt())
                .set(UserMfaFactorPO::getLastUsedAt, po.getLastUsedAt())
                .eq(UserMfaFactorPO::getId, po.getId())
                .eq(UserMfaFactorPO::getUserId, po.getUserId()));
    }

    private void deleteIdentity(UserIdentity identity) {
        identityMapper.delete(Wrappers.<UserIdentityPO>lambdaQuery()
                .eq(UserIdentityPO::getId, toLong(identity.getId()))
                .eq(UserIdentityPO::getUserId, toLong(identity.getUserId())));
    }

    private void deleteFactor(MfaFactor factor) {
        factorMapper.delete(Wrappers.<UserMfaFactorPO>lambdaQuery()
                .eq(UserMfaFactorPO::getId, toLong(factor.getId()))
                .eq(UserMfaFactorPO::getUserId, toLong(factor.getUserId())));
    }

    // ------------------------------------------------------------------ lookup 确认 / 释放

    private void registerLookupCallbacks(List<UserIdentity> reserved, ShardedId userId) {
        if (reserved.isEmpty()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (UserIdentity identity : reserved) {
                    try {
                        lookup.confirm(identity.getType(), identity.getIdentifier(), userId);
                    } catch (RuntimeException e) {
                        // 数据已提交，不能让调用方看到失败；遗留的 PENDING 由回收任务补确认
                        log.error("identity_lookup 确认失败，等待回收任务处理 type={}", identity.getType(), e);
                    }
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    reserved.forEach(i -> releaseQuietly(i, userId));
                }
            }
        });
    }

    private void releaseQuietly(UserIdentity identity, ShardedId userId) {
        try {
            lookup.release(identity.getType(), identity.getIdentifier(), userId);
        } catch (RuntimeException e) {
            log.warn("identity_lookup 释放失败，等待回收任务处理 type={}", identity.getType(), e);
        }
    }

    // 与 IdentityLookupGateway 同一处对接点：按项目里 ShardedId 的实际 API 调整
    private static Long toLong(ShardedId id) {
        return id.value();
    }
}