package com.ovelin.mall.common.sharding.core.api;

import com.ovelin.mall.common.sharding.core.ov.ShardId;

// 有输入，根据业务键确定性地算出分片
public interface ShardResolver {
    ShardId resolve(String identifier);
}