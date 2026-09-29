package com.ovelin.mall.sharding.starter.api.excutor.facade;

import com.ovelin.mall.sharding.starter.api.excutor.ShardedMyBatisPlusExecutor;
import com.ovelin.mall.sharding.starter.core.client.MyBatisPlusClient;

public abstract class BaseMyBatisPlusShardGroupExecutor extends BaseShardGroupExecutor<MyBatisPlusClient>{
    public BaseMyBatisPlusShardGroupExecutor(ShardedMyBatisPlusExecutor executor) {
        super(executor);
    }
}
