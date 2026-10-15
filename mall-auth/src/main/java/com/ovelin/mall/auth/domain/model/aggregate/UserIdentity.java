package com.ovelin.mall.auth.domain.model.aggregate;

import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifier;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifierFactory;
import com.ovelin.mall.common.exception.global.InvalidArgumentException;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

/**
 * 登录身份：LoginAccount 聚合内的实体。
 * 创建与状态修改方法均为包级可见，外部只能通过 {@link LoginAccount} 操作；
 * 修改方法返回"是否发生变化"，供聚合根决定是否记入变更集。
 */
@Getter
public class UserIdentity {

    private final ShardedId id;
    private final ShardedId userId;
    private final IdentityIdentifier identityIdentifier;
    private Instant verifiedAt;
    private Instant lastLoginAt;

    private UserIdentity(ShardedId id, ShardedId userId, IdentityIdentifier identityIdentifier,
                         Instant verifiedAt, Instant lastLoginAt) {
        this.id = id;
        this.userId = userId;
        this.identityIdentifier = identityIdentifier;
        this.verifiedAt = verifiedAt;
        this.lastLoginAt = lastLoginAt;
    }

    static UserIdentity register(
            ShardedId id,
            ShardedId userId,
            IdentityIdentifier identityIdentifier) {

        Objects.requireNonNull(id, "id 不能为空");
        Objects.requireNonNull(userId, "userId 不能为空");
        Objects.requireNonNull(identityIdentifier, "identityIdentifier 不能为空");

        if (!id.shardId().equals(userId.shardId())) {
            throw new InvalidArgumentException("身份记录必须与用户落在同一分片");
        }

        return new UserIdentity(id, userId, identityIdentifier, null, null);
    }

    /** 仅供持久化层还原，不做标准化和校验 */
    public static UserIdentity reconstitute(ShardedId id, ShardedId userId, String type,
                                            String identifier,
                                            Instant verifiedAt, Instant lastLoginAt) {
        return new UserIdentity(id, userId, IdentityIdentifierFactory.reconstitute(type, identifier), verifiedAt, lastLoginAt);
    }

    /** @return 是否发生变化（已验证过则不变） */
    boolean markVerified(Instant at) {
        if (verifiedAt != null) {
            return false;
        }
        verifiedAt = at;
        return true;
    }

    void recordLogin(Instant at) {
        this.lastLoginAt = at;
    }

    boolean matches(IdentityIdentifier identityIdentifier) {
        return this.identityIdentifier.equals(identityIdentifier);
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public boolean canReceiveCode() {
        return identityIdentifier.type().canReceiveCode() && isVerified();
    }

    public IdentityType getType(){
        return this.identityIdentifier.type();
    }
    public String getIdentifier(){
        return identityIdentifier.value();
    }


}