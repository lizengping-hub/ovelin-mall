package com.ovelin.mall.sharding.starter.api.excutor.facade;

import com.ovelin.mall.sharding.starter.api.excutor.ShardedMyBatisPlusExecutor;
import com.ovelin.mall.sharding.starter.core.client.MyBatisPlusClient;

public abstract class BaseMyBatisShardGroupExecutor extends BaseShardGroupExecutor<MyBatisPlusClient> {
    public BaseMyBatisShardGroupExecutor(ShardedMyBatisPlusExecutor executor) {
        super(executor);
    }
}
