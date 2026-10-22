package com.ovelin.mall.auth.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ovelin.mall.auth.domain.exception.IdentifierAlreadyTakenException;
import com.ovelin.mall.auth.domain.exception.MfaFactorAlreadyExistException;
import com.ovelin.mall.auth.domain.model.aggregate.LoginAccount;
import com.ovelin.mall.auth.domain.model.aggregate.UserMfaFactor;
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
import java.util.concurrent.atomic.AtomicBoolean;

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
        List<UserMfaFactor> factors = factorMapper.selectByUserId(uid)
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
        // 事务同步回调注册之后，回滚补偿由回调的 afterCompletion 负责，catch 不再重复处理
        AtomicBoolean callbacksRegistered = new AtomicBoolean(false);
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
                callbacksRegistered.set(true);
                writeRows(account);
            });
        } catch (RuntimeException e) {
            // 回调还没注册的失败（reserve / beginRelease 中途失败、事务没能开始）只能在这里补偿；
            // 回调已注册的，回滚时 afterCompletion 会处理，这里跳过，避免重复取消
            if (!callbacksRegistered.get()) {
                lookupAbort(reserved, releasing, userId);
            }
            throw e;
        }
        account.clearChanges();
    }
    // ------------------------------------------------------------------ 落库（同分片本地事务内）
    private void insertRows(LoginAccount account){
        account.addedIdentities().forEach(this::insertIdentity);
        account.addedFactors().forEach(this::insertFactor);
    }
    /** 顺序：因子删/改 → 身份删 → 身份改 → 身份增 → 因子增（因子引用的身份先于因子存在） */
    private void writeRows(LoginAccount account) {
        account.removedFactors().forEach(this::deleteFactor);
        account.dirtyFactors().forEach(this::updateFactor);
        account.removedIdentities().forEach(this::deleteIdentity);
        account.dirtyIdentities().forEach(this::updateIdentity);
        account.addedIdentities().forEach(i -> {
            UserIdentityPO po = UserIdentityPOConverter.toPO(i);
            try {
                identityMapper.insert(po);
            } catch (DuplicateKeyException e){
                if (identityMapper.selectById(po.getId()) == null){
                    // 如果不能查出来，说明被其它人抢占，一般不会到达
                    throw new IdentifierAlreadyTakenException(i.getIdentifier());
                }
                // 能查到，视为幂等
            }

        });
        account.addedFactors().forEach(f -> {
            UserMfaFactorPO po = MfaFactorPOConverter.toPO(f);
            try {
                factorMapper.insert(po);
            } catch (DuplicateKeyException e){
                if (identityMapper.selectById(po.getId()) == null){
                    // 如果不能查出来，说明被其它人抢占，一般不会到达
                    throw new MfaFactorAlreadyExistException(f);
                }
                // 能查到，视为幂等
            }

        });
    }

    private void insertIdentity(UserIdentity userIdentity){
        UserIdentityPO po = UserIdentityPOConverter.toPO(userIdentity);
        try {
            identityMapper.insert(po);
        } catch (DuplicateKeyException e){
            throw new IdentifierAlreadyTakenException(userIdentity.getIdentifier());
        }
    }

    private void insertFactor(UserMfaFactor factor){
        UserMfaFactorPO po = MfaFactorPOConverter.toPO(factor);
        try {
            factorMapper.insert(po);
        } catch (DuplicateKeyException e){
            throw new MfaFactorAlreadyExistException(factor);
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

    private void updateFactor(UserMfaFactor factor) {
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

    private void deleteFactor(UserMfaFactor factor) {
        factorMapper.delete(Wrappers.<UserMfaFactorPO>lambdaQuery()
                .eq(UserMfaFactorPO::getId, factor.getId().value())
                .eq(UserMfaFactorPO::getUserId, factor.getUserId().value()));
    }

    // ------------------------------------------------------------------ lookup 确认 / 释放


    private void registerLookupCallbacks(List<UserIdentity> reserved, List<UserIdentity> releasing, ShardedId userId) {
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
                        // 数据已提交，不能让调用方看到失败；遗留的 RESERVED 由回收任务处理
                        log.error("identity_lookup 确认占用失败，等待回收任务处理 type={}", identity.type(), e);
                    }
                }
                for (UserIdentity identity : releasing) {
                    try {
                        lookup.completeRelease(identity.getIdentifier(), userId);
                    } catch (RuntimeException e) {
                        // 数据已提交，不能让调用方看到失败；遗留的 RELEASING 由回收任务处理
                        log.error("identity_lookup 完成释放失败，等待回收任务处理 type={}", identity.type(), e);
                    }
                }
            }

            /**
             * 不能只靠 persistWithLookup 的 catch：save() 可能处在外层事务中，
             * 外层事务在 save() 返回之后才回滚或提交失败，那时 catch 早已走完，只有这个回调能覆盖。
             */
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    lookupAbort(reserved, releasing, userId);
                }
            }
        });
    }

    /** 事务未提交时的补偿：取消占位（RESERVED）、恢复释放（RELEASING）。幂等，可重复调用 */
    private void lookupAbort(List<UserIdentity> reserved, List<UserIdentity> releasing, ShardedId userId) {
        for (UserIdentity identity : reserved) {
            try {
                lookup.cancelReserve(identity.getIdentifier(), userId);
            } catch (RuntimeException e) {
                // 补偿失败不能掩盖原始异常；遗留的 RESERVED 由回收任务处理
                log.warn("identity_lookup 取消占用失败，等待回收任务处理 type={}", identity.type(), e);
            }
        }
        for (UserIdentity identity : releasing) {
            try {
                lookup.abortRelease(identity.getIdentifier(), userId);
            } catch (RuntimeException e) {
                // 补偿失败不能掩盖原始异常；遗留的 RELEASING 由回收任务处理
                log.warn("identity_lookup 取消释放失败，等待回收任务处理 type={}", identity.type(), e);
            }
        }
    }

}