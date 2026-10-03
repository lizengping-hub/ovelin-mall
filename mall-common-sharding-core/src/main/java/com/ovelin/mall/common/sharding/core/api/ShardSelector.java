package com.ovelin.mall.common.sharding.core.api;

import com.ovelin.mall.common.sharding.core.ov.ShardId;

public interface ShardSelector {
    ShardId select();

}

