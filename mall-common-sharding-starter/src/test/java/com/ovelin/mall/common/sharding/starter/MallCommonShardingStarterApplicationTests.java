package com.ovelin.mall.common.sharding.starter;


import com.ovelin.mall.common.sharding.core.api.ShardResolver;
import com.ovelin.mall.common.sharding.core.ov.ShardId;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.common.sharding.starter.mapper.UserIdentityMapper;
import com.ovelin.mall.common.sharding.starter.mapper.UserMapper;
import com.ovelin.mall.common.sharding.starter.po.IdentityPO;
import com.ovelin.mall.common.sharding.starter.po.UserPO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MallCommonShardingStarterApplicationTests {
    @Autowired
    UserMapper userMapper;
    @Autowired
    UserIdentityMapper userIdentityMapper;
    @Autowired
    ShardResolver shardResolver;
    @Test
    void contextLoads() throws SQLException {
        if (userMapper == null) {
            System.out.println("userMapper is null");
        } else {
            System.out.println("userMapper is not null");
        }

        ShardId shardId = shardResolver.currentShard();
        System.out.println("Current shard ID: " + shardId.value());
        long userId = ShardedId.of(shardId, System.currentTimeMillis()).value();
        long identityId = ShardedId.of(shardId, System.currentTimeMillis()).value();

        IdentityPO identity = new IdentityPO();
        identity.setId(identityId);
        identity.setUserId(userId);
        identity.setIdentityType("test");
        identity.setNormalizedIdentifier("test-identifier"+System.currentTimeMillis());
        identity.setIsPrimary(1);
        identity.setVerifiedAt(LocalDateTime.now());
        identity.setLastLoginAt(LocalDateTime.now());
        identity.setCreatedAt(LocalDateTime.now());
        identity.setUpdatedAt(identity.getCreatedAt());
        userIdentityMapper.insert(identity);
        System.out.println("Inserted user identity ID: " + identityId);
        IdentityPO identityPO = userIdentityMapper.selectById(identityId);
        System.out.println("Inserted user identity: " + identityPO);
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
        int inserted = userMapper.insert(user);

        System.out.println("Inserted user ID: " + userId);
        UserPO userPO = userMapper.selectById(userId);
        System.out.println("Inserted user: " + userPO);
        assertThat(inserted).isEqualTo(1);
    }

    @Test
    void testTransactionRollback() throws SQLException {
        long userId = ShardedId.of(ShardId.of(0), System.currentTimeMillis()).value();
        long identityId = ShardedId.of(ShardId.of(0), System.currentTimeMillis()).value();

        IdentityPO identity = new IdentityPO();
        identity.setId(identityId);
        identity.setUserId(userId);
        identity.setIdentityType("test");
        identity.setNormalizedIdentifier("test-identifier"+System.currentTimeMillis());
        identity.setIsPrimary(1);
        identity.setVerifiedAt(LocalDateTime.now());
        identity.setLastLoginAt(LocalDateTime.now());
        identity.setCreatedAt(LocalDateTime.now());
        identity.setUpdatedAt(identity.getCreatedAt());
        userIdentityMapper.insert(identity);
//        throw new SQLException("Simulated exception to test transaction rollback");
    }

}
