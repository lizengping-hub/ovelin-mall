package com.ovelin.mall.sharding.starter;

import com.ovelin.mall.sharding.starter.api.ShardedId;
import com.ovelin.mall.sharding.starter.api.ResolvedRoute;
import com.ovelin.mall.sharding.starter.api.ShardGroupKey;
import com.ovelin.mall.sharding.starter.core.ShardRouterImpl;
import com.ovelin.mall.sharding.starter.api.ShardingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MallShardingStarterApplicationTests {

    @Autowired
    ShardingProperties properties;

    @Test
    void contextLoads() {
    }

    @Test
    void shardGroupKeyBoundFromYamlStringWorks() {
        // 验证 yaml 中 "user" 这个字符串 key 能被自动转换成 ShardGroupKey record
        ShardRouterImpl shardRouter = new ShardRouterImpl(properties);
        ResolvedRoute route = shardRouter.route(new ShardGroupKey("user"), ShardedId.from(0L, 0L));
        assertThat(route.databaseName()).isEqualTo("user_database");
    }

}
