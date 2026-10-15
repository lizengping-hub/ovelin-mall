package com.ovelin.mall.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ovelin.mall.auth.infrastructure.persistence.AuthIntegrationTestSupport;
import com.ovelin.mall.auth.infrastructure.persistence.po.UserPasswordPO;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserPasswordMapperIT extends AuthIntegrationTestSupport {

    @Autowired
    UserPasswordMapper mapper;

    @Test
    @DisplayName("插入后按用户id查询")
    void insertAndSelectByUserId() {
        ShardedId userId = newId("user", 0);
        UserPasswordPO po = password(userId, "hash-1");
        po.setPasswordChangedAt(now());
        mapper.insert(po);

        UserPasswordPO actual = mapper.selectById(userId.value());

        assertThat(actual).isNotNull();
        assertThat(actual.getPasswordHash()).isEqualTo("hash-1");
        assertThat(actual.getPasswordChangedAt()).isEqualTo(po.getPasswordChangedAt());
        assertThat(actual.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("未设置的字段使用数据库默认值")
    void defaultValueShouldBeSetAfterInsert() {
        ShardedId userId = newId("user", 0);
        mapper.insert(password(userId, "hash-2"));

        UserPasswordPO actual = mapper.selectById(userId.value());

        assertThat(actual.getFailedAttempts()).isZero();
        assertThat(actual.getPasswordChangedAt()).isNotNull();
        assertThat(actual.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("更新时lockedUntil可以写回null")
    void updateCanSetLockedUntilToNull() {
        ShardedId userId = newId("user", 0);
        UserPasswordPO po = password(userId, "hash-3");
        po.setFailedAttempts(3);
        po.setLockedUntil(now().plusSeconds(600));
        mapper.insert(po);

        mapper.update(null, Wrappers.<UserPasswordPO>lambdaUpdate()
                .set(UserPasswordPO::getFailedAttempts, 0)
                .set(UserPasswordPO::getLockedUntil, null)
                .eq(UserPasswordPO::getUserId, userId.value()));

        UserPasswordPO actual = mapper.selectById(userId.value());
        assertThat(actual.getFailedAttempts()).isZero();
        assertThat(actual.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("同一用户重复插入违反主键")
    void inertOnePasswordTwiceThrowDuplicateKeyException() {
        ShardedId userId = newId("user", 0);
        mapper.insert(password(userId, "hash-4"));

        assertThatThrownBy(() -> mapper.insert(password(userId, "hash-5")))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    @DisplayName("deleteById只删除该用户")
    void deleteById_onlyDeleteCurrentUserData() {
        ShardedId user1 = newId("user", 0);
        ShardedId user2 = newId("user", 0);
        mapper.insert(password(user1, "hash-6"));
        mapper.insert(password(user2, "hash-7"));

        assertThat(mapper.deleteById(user1.value())).isEqualTo(1);

        assertThat(mapper.selectById(user1.value())).isNull();
        assertThat(mapper.selectById(user2.value())).isNotNull();
    }
    @Test
    @DisplayName("update_at 需要再更新后重置")
    void updateTimeChangeWhenUpdate() throws InterruptedException {
        ShardedId userId = newId("user", 0);
        UserPasswordPO po = password(userId, "hash-4");
        mapper.insert(po);

        UserPasswordPO loaded = mapper.selectById(po.getUserId());
        po.setPasswordHash("hash-5");
        mapper.updateById(po);
        UserPasswordPO loaded2 = mapper.selectById(po.getUserId());

        assertThat(loaded.getUpdatedAt()).isBefore(loaded2.getUpdatedAt());
    }

    private static UserPasswordPO password(ShardedId userId, String hash) {
        UserPasswordPO po = new UserPasswordPO();
        po.setUserId(userId.value());
        po.setPasswordHash(hash);
        return po;
    }
}