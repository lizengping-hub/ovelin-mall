package com.ovelin.mall.sharding.starter.interfaceTest;

import org.springframework.jdbc.core.JdbcTemplate;

public class test {
}

interface ShardedExecutor<C> {
}
interface ShardedJdbcExecutor extends ShardedExecutor<JdbcTemplate> {

}

class Test{
    ShardedExecutor<JdbcTemplate> executor;
    Test(){
        ShardedJdbcExecutor jdbcExecutor = new ShardedJdbcExecutor() {
            @Override
            public int hashCode() {
                return super.hashCode();
            }
        };

        executor = jdbcExecutor;
    }
}

