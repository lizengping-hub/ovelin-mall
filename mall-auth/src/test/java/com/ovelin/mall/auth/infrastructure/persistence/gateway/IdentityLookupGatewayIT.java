package com.ovelin.mall.auth.infrastructure.persistence.gateway;

import com.ovelin.mall.auth.domain.exception.IdentifierAlreadyTakenException;
import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.id.generator.api.IdGenerationCommand;
import com.ovelin.mall.id.generator.api.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class IdentityLookupGatewayIT {

    private static final IdentityType TYPE = IdentityType.EMAIL;
    private static final String IDENTIFIER = "a@example.com";

    @Autowired
    private IdentityLookupGateway gateway;
    @Autowired
    private IdGenerator idGenerator;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanTables() {
        jdbc.update("DELETE FROM identity_lookup");
    }

    @Test
    @DisplayName("Pending 状态的身份无法路由到用户")
    void pendingReservationIsNotVisibleToLoginRouting() {
        gateway.reserve(TYPE, IDENTIFIER, newUserId());

        assertThat(gateway.findUserId(TYPE, IDENTIFIER)).isEmpty();
    }

    @Test
    @DisplayName("已确认的身份可以路由到用户")
    void confirmedReservationRoutesToUser() {
        ShardedId userId = newUserId();
        gateway.reserve(TYPE, IDENTIFIER, userId);
        gateway.confirm(TYPE, IDENTIFIER, userId);

        assertThat(gateway.findUserId(TYPE, IDENTIFIER)).contains(userId);
    }

    @Test
    @DisplayName("任何状态的申请无法被其他用户抢占")
    void reserveByAnotherUserIsRejected() {
        ShardedId userId = newUserId();
        gateway.reserve(TYPE, IDENTIFIER, userId);

        assertThatThrownBy(() -> gateway.reserve(TYPE, IDENTIFIER, newUserId()))
                .isInstanceOf(IdentifierAlreadyTakenException.class);

        gateway.confirm(TYPE, IDENTIFIER, userId);
        assertThatThrownBy(() -> gateway.reserve(TYPE, IDENTIFIER, newUserId()))
                .isInstanceOf(IdentifierAlreadyTakenException.class);
    }

    @Test
    @DisplayName("一个用户重复抢占是幂等的")
    void reserveIsIdempotentForSameUserWhilePending() {
        ShardedId userId = newUserId();
        gateway.reserve(TYPE, IDENTIFIER, userId);

        gateway.reserve(TYPE, IDENTIFIER, userId);

        assertThat(count()).isEqualTo(1);
    }

    @Test
    @DisplayName("已经确认的预留即使被当前用户抢占也被拒绝")
    void reserveAfterConfirmIsRejectedEvenForSameUser() {
        ShardedId userId = newUserId();
        gateway.reserve(TYPE, IDENTIFIER, userId);
        gateway.confirm(TYPE, IDENTIFIER, userId);

        assertThatThrownBy(() -> gateway.reserve(TYPE, IDENTIFIER, userId))
                .isInstanceOf(IdentifierAlreadyTakenException.class);
    }

    @Test
    @DisplayName("重复确认时幂等的")
    void confirmIsIdempotent() {
        ShardedId userId = newUserId();
        gateway.reserve(TYPE, IDENTIFIER, userId);
        gateway.confirm(TYPE, IDENTIFIER, userId);

        gateway.confirm(TYPE, IDENTIFIER, userId);

        assertThat(gateway.findUserId(TYPE, IDENTIFIER)).contains(userId);
    }

    @Test
    @DisplayName("确认不存在的预留是报错的")
    void confirmWithoutReservationFails() {
        assertThatThrownBy(() -> gateway.confirm(TYPE, IDENTIFIER, newUserId()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void releaseOnlyAffectsOwner() {
        ShardedId owner = newUserId();
        gateway.reserve(TYPE, IDENTIFIER, owner);
        gateway.confirm(TYPE, IDENTIFIER, owner);

        gateway.release(TYPE, IDENTIFIER, newUserId());
        assertThat(gateway.findUserId(TYPE, IDENTIFIER)).contains(owner);

        gateway.release(TYPE, IDENTIFIER, owner);
        assertThat(gateway.findUserId(TYPE, IDENTIFIER)).isEmpty();
    }

    @Test
    void identifierCanBeReservedAgainAfterRelease() {
        ShardedId first = newUserId();
        gateway.reserve(TYPE, IDENTIFIER, first);
        gateway.release(TYPE, IDENTIFIER, first);

        ShardedId second = newUserId();
        gateway.reserve(TYPE, IDENTIFIER, second);
        gateway.confirm(TYPE, IDENTIFIER, second);

        assertThat(gateway.findUserId(TYPE, IDENTIFIER)).contains(second);
    }

    @Test
    void reclaimDeletesStalePendingWhenIdentityRowMissing() {
        gateway.reserve(TYPE, IDENTIFIER, newUserId());
        backdate(IDENTIFIER);

        int handled = gateway.reclaimStale(Duration.ofMinutes(30), 100, pending -> false);

        assertThat(handled).isEqualTo(1);
        assertThat(count()).isZero();
    }

    @Test
    void reclaimPromotesStalePendingWhenIdentityRowExists() {
        ShardedId userId = newUserId();
        gateway.reserve(TYPE, IDENTIFIER, userId);
        backdate(IDENTIFIER);

        gateway.reclaimStale(Duration.ofMinutes(30), 100, pending -> true);

        assertThat(gateway.findUserId(TYPE, IDENTIFIER)).contains(userId);
    }

    @Test
    void reclaimIgnoresRecentPendingAndConfirmed() {
        ShardedId confirmedUser = newUserId();
        gateway.reserve(TYPE, IDENTIFIER, confirmedUser);
        gateway.confirm(TYPE, IDENTIFIER, confirmedUser);
        backdate(IDENTIFIER);
        gateway.reserve(TYPE, "b@example.com", newUserId());

        int handled = gateway.reclaimStale(Duration.ofMinutes(30), 100, pending -> false);

        assertThat(handled).isZero();
        assertThat(count()).isEqualTo(2);
    }

    // ------------------------------------------------------------------

    private void backdate(String identifier) {
        jdbc.update("UPDATE identity_lookup SET created_at = DATE_SUB(NOW(3), INTERVAL 1 HOUR) "
                + "WHERE identity_type = ? AND identifier = ?", TYPE.getCode(), identifier);
    }

    private int count() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM identity_lookup", Integer.class);
        return n == null ? 0 : n;
    }

    /** 通过 IdGenerator 生成 ShardedId；若 IdGenerationCommand 的构造方式不同，只需改这一处 */
    private ShardedId newUserId() {
        return idGenerator.nextId(IdGenerationCommand.builder("auth_test_user").build());
    }
}