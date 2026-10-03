package com.ovelin.mall.common.sharding.starter.integration;

import com.ovelin.mall.common.sharding.core.ov.ShardId;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.common.sharding.starter.integration.po.ShardingTestUserPO;
import com.ovelin.mall.common.sharding.starter.test.TestSQLExecutionHook;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@SpringBootTest
public class ShardingTestUserPOIT extends AbstractShardingTestPOIT {
    @Test
    void testShardingTestUser() {
        for (int i = 0; i < 8; i++) {
            testShardingTestUser(ShardId.of(i));
        }
    }
    void testShardingTestUser(ShardId shardId) {
        long userId = ShardedId.of(shardId, System.currentTimeMillis()).value();
        ShardingTestUserPO user = new ShardingTestUserPO();
        user.setId(userId);
        user.setDisplayName("Test User " + System.currentTimeMillis());
        TestSQLExecutionHook.clear();
        userMapper.insert(user);
        assertThat(TestSQLExecutionHook.getActualSqls().size()).isEqualTo(1);
        ShardingTestUserPO userPoBYMapper = userMapper.selectById(userId);
        assertThat(TestSQLExecutionHook.getActualSqls().size()).isEqualTo(2);
        assertThat(userPoBYMapper).isNotNull();
        assertThat(userPoBYMapper.getId()).isEqualTo(userId);

        JdbcTemplate jdbcTemplate = unshardedJdbcTemplates.get("user-ds" + dataSourceIndexResolver.resolve(shardId, 2, 2));
        ShardingTestUserPO userPO1 = jdbcTemplate.queryForObject("SELECT id, display_name FROM sharding_test_user_" + tableIndexResolver.resolve(shardId, 2, 2) + " WHERE id = ?", USER_ROW_MAPPER, userId);
    }

}
