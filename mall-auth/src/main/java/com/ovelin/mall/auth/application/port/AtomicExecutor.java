package com.ovelin.mall.auth.application.port;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;

import java.util.function.Supplier;

public interface AtomicExecutor {            // domain 或 application 层的端口
    <T> T inShard(ShardedId anyIdInShard, Supplier<T> work);
}