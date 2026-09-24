package com.ovelin.mall.sharding.starter.api.excutor;

import org.springframework.jdbc.core.JdbcTemplate;

public interface ShardedJdbcExecutor extends ShardedExecutor<JdbcTemplate>{

}