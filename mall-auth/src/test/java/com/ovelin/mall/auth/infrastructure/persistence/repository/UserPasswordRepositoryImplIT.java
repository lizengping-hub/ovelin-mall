package com.ovelin.mall.auth.infrastructure.persistence.repository;

import com.ovelin.mall.auth.domain.model.aggregate.UserPassword;
import com.ovelin.mall.auth.domain.model.valueobject.EncodedPassword;
import com.ovelin.mall.auth.infrastructure.persistence.AuthIntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserPasswordRepositoryImplIT extends AuthIntegrationTestSupport {

    @Autowired
    UserPasswordRepositoryImpl repository;

    @Nested
    @DisplayName("add / findByUserId")
    class AddAndFind {

        @Test
        @DisplayName("add 后能完整还原")
        void restoresAfterAdd() {
            UserPassword password = savedPassword("hash-1");

            UserPassword actual = repository.findByUserId(password.getUserId()).orElseThrow();

            assertThat(actual).usingRecursiveComparison().isEqualTo(password);
        }

        @Test
        @DisplayName("记录不存在时返回空")
        void returnsEmptyWhenAbsent() {
            assertThat(repository.findByUserId(newId("user", 0))).isEmpty();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("持久化失败计数与锁定状态")
        void persistsFailureAndLock() {
            UserPassword password = savedPassword("hash-2");
            Instant failAt = now();

            password.recordFailure(1, Duration.ofMinutes(10), failAt);
            repository.update(password);

            UserPassword actual = reload(password);
            assertThat(actual).usingRecursiveComparison().isEqualTo(password);
            assertThat(actual.isLocked(failAt)).isTrue();
        }

        @Test
        @DisplayName("认证成功后清零失败计数，lockedUntil 写回 null")
        void clearsFailureAndLockOnSuccess() {
            UserPassword password = savedPassword("hash-3");
            password.recordFailure(1, Duration.ofMinutes(10), now());
            repository.update(password);

            password.recordSuccess();
            repository.update(password);

            UserPassword actual = reload(password);
            assertThat(actual.getFailedAttempts()).isZero();
            assertThat(actual.getLockedUntil()).isNull();
        }

        @Test
        @DisplayName("持久化修改后的密码哈希与修改时间")
        void persistsPasswordChange() {
            UserPassword password = savedPassword("old-hash");
            Instant changedAt = now().plusSeconds(60);

            password.change(new EncodedPassword("new-hash"), changedAt);
            repository.update(password);

            UserPassword actual = reload(password);
            assertThat(actual.getEncodedPassword()).isEqualTo("new-hash");
            assertThat(actual.getPasswordChangedAt()).isEqualTo(changedAt);
        }
    }

    @Nested
    @DisplayName("deleteByUserId")
    class Delete {

        @Test
        @DisplayName("只删除目标用户，不影响其他用户")
        void deletesOnlyTargetUser() {
            UserPassword target = savedPassword("h1");
            UserPassword other = savedPassword("h2");

            repository.deleteByUserId(target.getUserId());

            assertThat(repository.findByUserId(target.getUserId())).isEmpty();
            assertThat(repository.findByUserId(other.getUserId())).isPresent();
        }
    }

    /** 新建并落库一条密码记录，每次使用独立的 userId。 */
    private UserPassword savedPassword(String encodedPassword) {
        UserPassword password = UserPassword.create(newId("user", 0), new EncodedPassword(encodedPassword), now());
        repository.add(password);
        return password;
    }

    private UserPassword reload(UserPassword password) {
        return repository.findByUserId(password.getUserId()).orElseThrow();
    }
}