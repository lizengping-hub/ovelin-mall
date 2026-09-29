package com.ovelin.mall.sharding.starter.api.facade;

import com.ovelin.mall.sharding.starter.api.excutor.ShardedMyBatisPlusExecutor;
import com.ovelin.mall.sharding.starter.api.excutor.facade.BaseMyBatisPlusShardGroupExecutor;
import com.ovelin.mall.sharding.starter.api.router.ShardGroupKey;
import org.springframework.stereotype.Component;

@Component
public class UserShardGroupExecutorImpl extends BaseMyBatisPlusShardGroupExecutor implements UserShardGroupExecutor {
    public UserShardGroupExecutorImpl(ShardedMyBatisPlusExecutor executor) {
        super(executor);

    }
    @Override
    protected ShardGroupKey getShardGroupKey() {
        return new ShardGroupKey("user");
    }
}
