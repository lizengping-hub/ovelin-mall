package com.ovelin.mall.auth.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ovelin.mall.auth.domain.exception.IdentifierAlreadyTakenException;
import com.ovelin.mall.auth.domain.model.aggregate.LoginAccount;
import com.ovelin.mall.auth.domain.model.aggregate.MfaFactor;
import com.ovelin.mall.auth.domain.model.aggregate.UserIdentity;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifier;
import com.ovelin.mall.auth.domain.repository.LoginAccountRepository;


import com.ovelin.mall.auth.infrastructure.persistence.convertor.MfaFactorPOConverter;
import com.ovelin.mall.auth.infrastructure.persistence.convertor.UserIdentityPOConverter;
import com.ovelin.mall.auth.infrastructure.persistence.gateway.IdentityLookupGateway;
import com.ovelin.mall.auth.infrastructure.persistence.mapper.UserIdentityMapper;

import com.ovelin.mall.auth.infrastructure.persistence.mapper.UserMfaFactorMapper;
import com.ovelin.mall.auth.infrastructure.persistence.po.UserIdentityPO;
import com.ovelin.mall.auth.infrastructure.persistence.po.UserMfaFactorPO;
import com.ovelin.mall.common.sharding.core.api.ShardResolver;
import com.ovelin.mall.common.sharding.core.ov.ShardId;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
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
    private final ShardResolver shardResolver;

    @Override
    public Optional<LoginAccount> findByUserId(ShardedId userId) {
        Long uid = userId.value();
        List<UserIdentity> identities = identityMapper.selectByUserId(uid)
                .stream().map(UserIdentityPOConverter::toDomain).toList();
        if (identities.isEmpty()) {
            return Optional.empty();
        }
        List<MfaFactor> factors = factorMapper.selectByUserId(uid)
                .stream().map(MfaFactorPOConverter::toDomain).toList();
        return Optional.of(LoginAccount.reconstitute(userId, identities, factors));
    }

    @Override
    public Optional<ShardedId> findUserIdByIdentity(IdentityIdentifier identityIdentifier) {
        return lookup.findUserId(identityIdentifier);
    }

    /**
     * 仅在注册场景下使用
     */
    @Override
    public void add(LoginAccount account) {
        if (!account.hasChanges()) return;
        save(account,canSimpleInsert(account));
    }

    @Override
    public void update(LoginAccount account) {
        if (!account.hasChanges()) return;
        save(account, false);
    }

    private void save(LoginAccount account, boolean simplerInsert){
        if (simplerInsert) simpleInsert(account);
        else persistWithLookup(account);
    }
    /**
     * 检查是否是使用单个身份注册的场景，仅限 public void insert(LoginAccount account)
     * */
    private boolean canSimpleInsert(LoginAccount account){
        List<UserIdentity> identities = account.addedIdentities();
        if (identities.size() == 1){
            UserIdentity identity = identities.getFirst();
            ShardId shardId = shardResolver.resolve(identity.type().getCode(), identity.value());
            if (shardId.equals(account.getUserId().shardId())){
                return true;
            }
        }
        return false;
    }
    /**
     * 用于初次，使用单个身份注册的场景，仅限 public void insert(LoginAccount account)
     */
    private void simpleInsert(LoginAccount account){
        UserIdentity identity = account.addedIdentities().getFirst();
        tx.executeWithoutResult(s -> {
            lookup.insertConfirmed(identity.getIdentifier(), account.getUserId());
            insertRows(account);
        });
        account.clearChanges();
    }

    private void persistWithLookup(LoginAccount account) {

        ShardedId userId = account.getUserId();
        List<UserIdentity> reserved = new ArrayList<>();
        List<UserIdentity> releasing = new ArrayList<>();
        try {
            for (UserIdentity identity : account.addedIdentities()) {
                lookup.reserve(identity.getIdentifier(), userId);
                reserved.add(identity);
            }
            for (UserIdentity identity : account.removedIdentities()) {
                lookup.beginRelease(identity.getIdentifier(), userId);
                releasing.add(identity);
            }
            tx.executeWithoutResult(status -> {
                registerLookupCallbacks(reserved, releasing, userId);
                writeRows(account);
            });
        } catch (RuntimeException e) {
            // 幂等：回滚回调也会释放，这里兜住"事务都没能开始"的情况
            registerLookupAbort(reserved, releasing, userId);
            throw e;
        }
        account.clearChanges();
    }
    // ------------------------------------------------------------------ 落库（同分片本地事务内）
    private void insertRows(LoginAccount account){
        account.addedIdentities().forEach(this::insertIdentity);
        account.addedFactors().forEach(f -> factorMapper.insert(MfaFactorPOConverter.toPO(f)));
    }
    /** 顺序：因子删/改 → 身份删 → 身份改 → 身份增 → 因子增（因子引用的身份先于因子存在） */
    private void writeRows(LoginAccount account) {
        account.removedFactors().forEach(this::deleteFactor);
        account.dirtyFactors().forEach(this::updateFactor);
        account.removedIdentities().forEach(this::deleteIdentity);
        account.dirtyIdentities().forEach(this::updateIdentity);
        account.addedIdentities().forEach(i -> identityMapper.insert(UserIdentityPOConverter.toPO(i)));
        account.addedFactors().forEach(f -> factorMapper.insert(MfaFactorPOConverter.toPO(f)));
    }

    private void insertIdentity(UserIdentity userIdentity){
        UserIdentityPO po = UserIdentityPOConverter.toPO(userIdentity);
        try {
            identityMapper.insert(po);
        } catch (DuplicateKeyException e){
            throw new IdentifierAlreadyTakenException(userIdentity.getIdentifier());
        }
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
                .eq(UserIdentityPO::getId, identity.getId().value())
                .eq(UserIdentityPO::getUserId, identity.getUserId().value()));
    }

    private void deleteFactor(MfaFactor factor) {
        factorMapper.delete(Wrappers.<UserMfaFactorPO>lambdaQuery()
                .eq(UserMfaFactorPO::getId, factor.getId().value())
                .eq(UserMfaFactorPO::getUserId, factor.getUserId().value()));
    }

    // ------------------------------------------------------------------ lookup 确认 / 释放

    private void registerLookupCallbacks(List<UserIdentity> reserved,  List<UserIdentity> releasing, ShardedId userId) {
        if (reserved.isEmpty() && releasing.isEmpty()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (UserIdentity identity : reserved) {
                    try {
                        lookup.confirm(identity.getIdentifier(), userId);
                    } catch (RuntimeException e) {
                        // 数据已提交，不能让调用方看到失败；遗留的 PENDING 由回收任务补确认
                        log.error("identity_lookup 确认占用失败，等待回收任务处理 type={}", identity.type(), e);
                    }
                }
                for (UserIdentity identity : releasing){
                    try {
                        lookup.completeRelease(identity.getIdentifier(), userId);
                    } catch (RuntimeException e) {
                        // 数据已提交，不能让调用方看到失败；遗留的 PENDING 由回收任务补确认
                        log.error("identity_lookup 完成释放失败，等待回收任务处理 type={}", identity.type(), e);
                    }
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    registerLookupAbort(reserved, releasing, userId);
                }
            }
        });
    }
    private void registerLookupAbort(List<UserIdentity> reserved,  List<UserIdentity> releasing, ShardedId userId){
        for (UserIdentity identity : reserved) {
            try {
                lookup.cancelReserve(identity.getIdentifier(), userId);
            } catch (RuntimeException e) {
                // 数据已提交，不能让调用方看到失败；遗留的 PENDING 由回收任务补确认
                log.error("identity_lookup 取消占用失败，等待回收任务处理 type={}", identity.type(), e);
            }
        }
        for (UserIdentity identity : releasing){
            try {
                lookup.abortRelease(identity.getIdentifier(), userId);
            } catch (RuntimeException e) {
                // 数据已提交，不能让调用方看到失败；遗留的 PENDING 由回收任务补确认
                log.error("identity_lookup 取消释放释放失败，等待回收任务处理 type={}", identity.type(), e);
            }
        }
    }
    private void releaseQuietly(UserIdentity identity, ShardedId userId) {
        try {
            lookup.cancelReserve(identity.getIdentifier(), userId);
        } catch (RuntimeException e) {
            log.warn("identity_lookup 释放失败，等待回收任务处理 type={}", identity.type(), e);
        }
    }

}