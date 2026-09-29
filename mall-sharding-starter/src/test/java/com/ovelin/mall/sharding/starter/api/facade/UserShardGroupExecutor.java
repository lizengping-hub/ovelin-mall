package com.ovelin.mall.sharding.starter.api.facade;

import com.ovelin.mall.sharding.starter.api.excutor.facade.ShardGroupExecutor;
import com.ovelin.mall.sharding.starter.core.client.MyBatisPlusClient;

public interface UserShardGroupExecutor extends ShardGroupExecutor<MyBatisPlusClient> {
}
