package com.ovelin.mall.auth.domain.repository;

import com.ovelin.mall.auth.domain.model.aggregate.LoginAccount;
import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifier;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;

import java.util.Optional;

/**
 * LoginAccount 聚合仓储。
 * save 负责"同分片原子保存"以及登录标识全局唯一（identity_lookup）的占位与确认，
 * 应用层看不到事务 / outbox / lookup 协议的任何细节。
 */
public interface LoginAccountRepository {

    Optional<LoginAccount> findByUserId(ShardedId userId);

    /** 登录路由：标识 → userId（只认已确认的记录） */
    Optional<ShardedId> findUserIdByIdentity(IdentityIdentifier identityIdentifier);

    /**
     * 保存聚合内的全部变更（新增 / 修改 / 删除的身份与因子），成功后清空聚合的变更集。
     *
     * @throws com.ovelin.mall.auth.domain.exception.IdentifierAlreadyTakenException 新增的登录标识已被占用
     */
    void save(LoginAccount account);
}