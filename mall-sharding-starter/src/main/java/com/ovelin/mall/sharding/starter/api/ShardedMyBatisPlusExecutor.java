package com.ovelin.mall.sharding.starter.api;

import com.ovelin.mall.sharding.starter.api.excutor.ShardedExecutor;
import com.ovelin.mall.sharding.starter.core.client.MyBatisPlusClient;

public interface ShardedMyBatisPlusExecutor extends ShardedExecutor<MyBatisPlusClient> {
}
