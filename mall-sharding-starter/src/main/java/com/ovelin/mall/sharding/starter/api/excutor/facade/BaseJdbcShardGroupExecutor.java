package com.ovelin.mall.sharding.starter.api.excutor.facade;

import com.ovelin.mall.sharding.starter.api.excutor.ShardedJdbcExecutor;
import org.springframework.jdbc.core.JdbcTemplate;

public abstract class BaseJdbcShardGroupExecutor extends BaseShardGroupExecutor<JdbcTemplate>{
    public BaseJdbcShardGroupExecutor(ShardedJdbcExecutor executor) {
        super(executor);
    }
}
