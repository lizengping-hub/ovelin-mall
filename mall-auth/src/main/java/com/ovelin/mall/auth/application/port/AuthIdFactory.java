package com.ovelin.mall.auth.application.port;

import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;

/** 把"按标识哈希得到分片 + 生成分片 ID"封装起来，实现里调用你现有的 ShardResolver 和 IdGeneratorAppService */
public interface AuthIdFactory {

    /** 注册：对 (type, identifier) 哈希得到 shard，生成落在该 shard 的 userId */
    ShardedId newUserId(IdentityType type, String normalizedIdentifier);

    /** 生成与 userId 同分片的 identity id */
    ShardedId newIdentityId(ShardedId userId);

    /** 生成与 userId 同分片的 mfa factor id */
    ShardedId newFactorId(ShardedId userId);
}