package com.ovelin.mall.auth.infrastructure.persistence.mapper;


import com.ovelin.mall.auth.infrastructure.persistence.AuthIntegrationTestSupport;
import com.ovelin.mall.auth.infrastructure.persistence.po.IdentityLookupPO;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityLookupMapperIT extends AuthIntegrationTestSupport {

    @Autowired
    IdentityLookupMapper mapper;

    @Test
    @DisplayName("insert后能按类型和标识查到")
    void insertAndQueryByTypeAndIdentifier() {
        ShardedId userId = newId("user", 0);
        mapper.insert(lookup("phone", "13900000001", userId));

        IdentityLookupPO actual = mapper.selectByTypeAndIdentifier("phone", "13900000001");

        assertThat(actual).isNotNull();
        assertThat(actual.getUserId()).isEqualTo(userId.value());
        assertThat(actual.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("查不存在的标识返回null")
    void queryNonExistentIdentifierReturnsNull() {
        assertThat(mapper.selectByTypeAndIdentifier("phone", "13900000002")).isNull();
    }

    @Test
    @DisplayName("类型不同的相同标识互不影响")
    void differentTypesWithSameIdentifierDoNotInterfere() {
        ShardedId user1 = newId("user", 0);
        ShardedId user2 = newId("user", 0);
        mapper.insert(lookup("username", "alice", user1));
        mapper.insert(lookup("email", "alice", user2));

        assertThat(mapper.selectByTypeAndIdentifier("username", "alice").getUserId()).isEqualTo(user1.value());
        assertThat(mapper.selectByTypeAndIdentifier("email", "alice").getUserId()).isEqualTo(user2.value());
    }

    @Test
    @DisplayName("重复插入违反主键")
    void duplicateInsertViolatesPrimaryKey() {
        mapper.insert(lookup("phone", "13900000003", newId("user", 0)));

        IdentityLookupPO duplicate = lookup("phone", "13900000003", newId("user", 1));

        assertThatThrownBy(() -> mapper.insert(duplicate)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    @DisplayName("删除后可以重新占用")
    void canReclaimAfterDeletion() {
        mapper.insert(lookup("phone", "13900000004", newId("user", 0)));

        int deleted = mapper.deleteByTypeAndIdentifier("phone", "13900000004");

        assertThat(deleted).isEqualTo(1);
        assertThat(mapper.selectByTypeAndIdentifier("phone", "13900000004")).isNull();
        mapper.insert(lookup("phone", "13900000004", newId("user", 1)));
    }

    @Test
    @DisplayName("删除不存在的标识返回0")
    void deleteNonExistentIdentifierReturnsZero() {
        assertThat(mapper.deleteByTypeAndIdentifier("phone", "13900000005")).isZero();
    }

    private static IdentityLookupPO lookup(String type, String identifier, ShardedId userId) {
        IdentityLookupPO po = new IdentityLookupPO();
        po.setIdentityType(type);
        po.setIdentifier(identifier);
        po.setUserId(userId.value());
        return po;
    }
}
