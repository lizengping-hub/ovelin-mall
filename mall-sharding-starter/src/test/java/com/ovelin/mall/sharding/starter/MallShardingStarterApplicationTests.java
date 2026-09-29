package com.ovelin.mall.sharding.starter;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.sharding.starter.api.facade.UserShardGroupExecutor;
import com.ovelin.mall.sharding.starter.api.router.ResolvedRoute;
import com.ovelin.mall.sharding.starter.api.router.ShardGroupKey;
import com.ovelin.mall.sharding.starter.core.ShardRouterImpl;
import com.ovelin.mall.sharding.starter.api.router.ShardingProperties;
import com.ovelin.mall.sharding.starter.mapper.UserMapper;
import com.ovelin.mall.sharding.starter.po.UserPO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MallShardingStarterApplicationTests {
    @Autowired
    UserShardGroupExecutor userShardGroupExecutor;
    @Test
    void contextLoads() {
        if (userShardGroupExecutor == null) {
            System.out.println("userShardGroupExecutor is null");
        } else {
            System.out.println("userShardGroupExecutor is not null");
        }
        long userId = ShardedId.of(0, System.currentTimeMillis()).value();
        UserPO user = new UserPO();
        user.setId(userId);
        user.setUsername("test-user");
        user.setPhoneE164("+8613800138000");
        user.setPhoneRawInput("13800138000");
        user.setEmail("test-user@example.com");
        user.setStatus(1);
        user.setRegisterSource("test");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(user.getCreatedAt());

        int inserted = userShardGroupExecutor.execute(
                ShardedId.fromId(userId),
                (client, route) ->
                        client.mapper(UserMapper.class).insert(user)
        );
        System.out.println("Inserted user ID: " + userId);
        UserPO userPO = userShardGroupExecutor.execute(
                ShardedId.fromId(userId),
                (client, route) -> client.mapper(UserMapper.class).selectById(userId)
        );
        System.out.println("Inserted user: " + userPO);
        assertThat(inserted).isEqualTo(1);
    }

    @Autowired
    ShardingProperties properties;

    @Test
    void shardGroupKeyBoundFromYamlStringWorks() {
        // 验证 yaml 中 "user" 这个字符串 key 能被自动转换成 ShardGroupKey record
        ShardRouterImpl shardRouter = new ShardRouterImpl(properties);
        ResolvedRoute route = shardRouter.route(new ShardGroupKey("user"), ShardedId.of(0, 0L));
        assertThat(route.databaseName()).isEqualTo("user_database");
    }

}
