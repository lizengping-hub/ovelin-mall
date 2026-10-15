package com.ovelin.mall.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ovelin.mall.auth.infrastructure.persistence.AuthIntegrationTestSupport;
import com.ovelin.mall.auth.infrastructure.persistence.po.UserIdentityPO;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserIdentityMapperIT extends AuthIntegrationTestSupport {

    @Autowired
    UserIdentityMapper mapper;

    @Test
    @DisplayName("insert后按userId查询_字段完整")
    void insertAndQueryByUserIdWithCompleteFields() {
        ShardedId userId = newId("user", 0);
        UserIdentityPO po = identity(newId("user_identity", 0), userId, "phone", "13800000000");
        po.setVerifiedAt(now());

        mapper.insert(po);

        List<UserIdentityPO> found = mapper.selectByUserId(userId.value());
        assertThat(found).hasSize(1);
        UserIdentityPO actual = found.getFirst();
        assertThat(actual.getId()).isEqualTo(po.getId());
        assertThat(actual.getUserId()).isEqualTo(userId.value());
        assertThat(actual.getIdentityType()).isEqualTo("phone");
        assertThat(actual.getIdentifier()).isEqualTo("13800000000");
        assertThat(actual.getVerifiedAt()).isEqualTo(po.getVerifiedAt());
        assertThat(actual.getLastLoginAt()).isNull();
    }

    @Test
    @DisplayName("createdAt和updatedAt由数据库填充")
    void createdAtAndUpdatedAtAreFilledByDatabase() {
        ShardedId userId = newId("user", 0);
        mapper.insert(identity(newId("user_identity", 0), userId, "email", "a@example.com"));

        UserIdentityPO actual = mapper.selectByUserId(userId.value()).get(0);

        assertThat(actual.getCreatedAt()).isNotNull();
        assertThat(actual.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("按userId查询只返回该用户的身份")
    void queryByUserIdReturnsOnlyThatUsersIdentities() {
        ShardedId user1 = newId("user", 0);
        ShardedId user2 = newId("user", 0);
        mapper.insert(identity(newId("user_identity", 0), user1, "phone", "13800000001"));
        mapper.insert(identity(newId("user_identity", 0), user1, "email", "u1@example.com"));
        mapper.insert(identity(newId("user_identity", 0), user2, "phone", "13800000002"));

        assertThat(mapper.selectByUserId(user1.value())).hasSize(2);
        assertThat(mapper.selectByUserId(user2.value())).hasSize(1);
    }

    @Test
    @DisplayName("update能把字段写回null")
    void updateCanSetFieldToNull() {
        ShardedId userId = newId("user", 0);
        UserIdentityPO po = identity(newId("user_identity", 0), userId, "phone", "13800000003");
        po.setVerifiedAt(now());
        mapper.insert(po);

        mapper.update(null, Wrappers.<UserIdentityPO>lambdaUpdate()
                .set(UserIdentityPO::getVerifiedAt, null)
                .eq(UserIdentityPO::getId, po.getId())
                .eq(UserIdentityPO::getUserId, userId.value()));

        assertThat(mapper.selectByUserId(userId.value()).get(0).getVerifiedAt()).isNull();
    }

    @Test
    @DisplayName("同分片内相同标识违反唯一键")
    void sameIdentifierInSameShardViolatesUniqueKey() {
        ShardedId userId = newId("user", 0);
        mapper.insert(identity(newId("user_identity", 0), userId, "phone", "13800000004"));

        UserIdentityPO duplicate = identity(newId("user_identity", 0), userId, "phone", "13800000004");

        assertThatThrownBy(() -> mapper.insert(duplicate)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    @DisplayName("不同分片内相同标识不冲突_全局唯一要靠lookup()")
    void sameIdentifierInDifferentShardsDoesNotConflict() {
        ShardedId user0 = newId("user", 0);
        ShardedId user1 = newId("user", 1);

        mapper.insert(identity(newId("user_identity", 0), user0, "phone", "13800000005"));
        mapper.insert(identity(newId("user_identity", 1), user1, "phone", "13800000005"));

        assertThat(mapper.selectByUserId(user0.value())).hasSize(1);
        assertThat(mapper.selectByUserId(user1.value())).hasSize(1);
    }

    @Test
    @DisplayName("deleteByUserId只删除该用户")
    void deleteByUserIdOnlyDeletesThatUser() {
        ShardedId user1 = newId("user", 0);
        ShardedId user2 = newId("user", 0);
        mapper.insert(identity(newId("user_identity", 0), user1, "phone", "13800000006"));
        mapper.insert(identity(newId("user_identity", 0), user1, "email", "d1@example.com"));
        mapper.insert(identity(newId("user_identity", 0), user2, "phone", "13800000007"));

        int deleted = mapper.deleteByUserId(user1.value());

        assertThat(deleted).isEqualTo(2);
        assertThat(mapper.selectByUserId(user1.value())).isEmpty();
        assertThat(mapper.selectByUserId(user2.value())).hasSize(1);
    }

    private static UserIdentityPO identity(ShardedId id, ShardedId userId, String type, String identifier) {
        UserIdentityPO po = new UserIdentityPO();
        po.setId(id.value());
        po.setUserId(userId.value());
        po.setIdentityType(type);
        po.setIdentifier(identifier);
        return po;
    }
}