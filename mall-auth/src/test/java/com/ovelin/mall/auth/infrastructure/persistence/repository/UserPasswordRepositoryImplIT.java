package com.ovelin.mall.auth.infrastructure.persistence.repository;

import com.ovelin.mall.auth.domain.model.aggregate.UserPassword;
import com.ovelin.mall.auth.infrastructure.persistence.AuthIntegrationTestSupport;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserPasswordRepositoryImplIT extends AuthIntegrationTestSupport {

    @Autowired
    UserPasswordRepositoryImpl repository;

    @Test
    void add后能还原() {
        ShardedId userId = newId("user", 0);
        UserPassword password = UserPassword.create(userId, "hash-1", now());
        repository.add(password);

        UserPassword actual = repository.findByUserId(userId).orElseThrow();

        assertThat(actual).usingRecursiveComparison().isEqualTo(password);
    }

    @Test
    void 查不到时返回空() {
        assertThat(repository.findByUserId(newId("user", 0))).isEmpty();
    }

    @Test
    void update持久化失败计数和锁定_解锁后写回null() {
        ShardedId userId = newId("user", 0);
        UserPassword password = UserPassword.create(userId, "hash-2", now());
        repository.add(password);

        Instant failAt = now();
        password.recordFailure(1, Duration.ofMinutes(10), failAt);
        repository.update(password);
        UserPassword locked = repository.findByUserId(userId).orElseThrow();
        assertThat(locked.isLocked(failAt)).isTrue();

        password.recordSuccess();
        repository.update(password);
        UserPassword unlocked = repository.findByUserId(userId).orElseThrow();
        assertThat(unlocked.getLockedUntil()).isNull();
        assertThat(unlocked.getFailedAttempts()).isZero();
    }

    @Test
    void update持久化改密码() {
        ShardedId userId = newId("user", 0);
        UserPassword password = UserPassword.create(userId, "old-hash", now());
        repository.add(password);

        Instant changedAt = now().plusSeconds(60);
        password.change("new-hash", changedAt);
        repository.update(password);

        UserPassword actual = repository.findByUserId(userId).orElseThrow();
        assertThat(actual.getPasswordHash()).isEqualTo("new-hash");
        assertThat(actual.getPasswordChangedAt()).isEqualTo(changedAt);
    }

    @Test
    void deleteByUserId只删除该用户() {
        ShardedId user1 = newId("user", 0);
        ShardedId user2 = newId("user", 0);
        repository.add(UserPassword.create(user1, "h1", now()));
        repository.add(UserPassword.create(user2, "h2", now()));

        repository.deleteByUserId(user1);

        assertThat(repository.findByUserId(user1)).isEmpty();
        assertThat(repository.findByUserId(user2)).isPresent();
    }
}