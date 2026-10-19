package com.ovelin.mall.auth.infrastructure.persistence.gateway;

import com.ovelin.mall.auth.domain.exception.IdentifierAlreadyTakenException;
import com.ovelin.mall.auth.domain.exception.IdentifierNotExistException;
import com.ovelin.mall.auth.domain.exception.IdentifierNotOwnedByUserException;
import com.ovelin.mall.auth.domain.exception.InvalidIdentifierStatusException;
import com.ovelin.mall.auth.domain.model.enums.IdentityLookupStatus;
import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import com.ovelin.mall.auth.domain.model.identityIdentifier.EmailIdentifier;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifier;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifierFactory;
import com.ovelin.mall.auth.infrastructure.persistence.mapper.IdentityLookupMapper;
import com.ovelin.mall.auth.infrastructure.persistence.po.IdentityLookupPO;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.id.generator.api.IdGenerationCommand;
import com.ovelin.mall.id.generator.api.IdGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * IdentityLookupGateway 集成测试（真实库 + 分片）。
 * 各 @Nested 组之间、用例之间互相独立，不依赖执行顺序；每个用例前后都会清空 identity_lookup。
 * 编号只是阅读顺序：状态机正向流程 → 非法转换 → 回收 → updated_at → 事务边界 → 并发。
 */
@SpringBootTest
class IdentityLookupGatewayIT {

    private static final IdentityIdentifier EMAIL_IDENTITY_IDENTIFIER =
            EmailIdentifier.from("a@example.com");
    private static final IdentityIdentifier ANOTHER_EMAIL_IDENTIFIER =
            EmailIdentifier.from("b@example.com");

    @Autowired
    private IdentityLookupGateway gateway;
    @Autowired
    private IdentityLookupMapper mapper;
    @Autowired
    private IdGenerator idGenerator;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private TransactionTemplate tx;

    @BeforeEach
    @AfterEach
    void cleanTables() {
        jdbc.update("DELETE FROM identity_lookup");
    }

    // =====================================================================
    // 0. reserve
    // =====================================================================
    @Nested
    @DisplayName("0. 申请占位 reserve")
    class Reserve {

        @Test
        @DisplayName("RESERVED 状态的占位记录对登录路由不可见")
        void reservedReservationIsNotVisibleToLoginRouting() {
            ShardedId userId = newUserId();

            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);

            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.RESERVED);
            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).isEmpty();
        }

        @Test
        @DisplayName("同一用户对同一标识在 RESERVED 期间重复 reserve，视为幂等成功")
        void reserveIsIdempotentForSameUserWhileReserved() {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);

            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);

            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.RESERVED);
            assertThat(count()).isEqualTo(1);
            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).isEmpty();
        }
        @Test
        @DisplayName("相同值不同类型的身份可以同时占用")
        void sameValueWithDifferentTypeIsAllowed() {
            ShardedId userId = newUserId();

            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);

            IdentityIdentifier sameValueIdentifier = IdentityIdentifierFactory.reconstitute(IdentityType.OAUTH.getCode(), EMAIL_IDENTITY_IDENTIFIER.value());
            gateway.reserve(sameValueIdentifier, userId);

            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.RESERVED);
            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).isEmpty();

            assertLookup(sameValueIdentifier, userId, IdentityLookupStatus.RESERVED);
            assertThat(gateway.findUserId(sameValueIdentifier)).isEmpty();
        }

        @Test
        @DisplayName("标识已 ACTIVE 时，同一用户重新 reserve 被拒绝，状态保持 ACTIVE")
        void reserveActiveIdentifierIsRejectedAndKeepsActive() {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);

            assertThatThrownBy(() -> gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId))
                    .isInstanceOf(InvalidIdentifierStatusException.class);

            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.ACTIVE);
        }

        @Test
        @DisplayName("标识 RELEASING 时，同一用户重新 reserve 被拒绝，状态保持 RELEASING")
        void reserveReleasingIdentifierIsRejectedAndKeepsReleasing() {
            ShardedId userId = newReleasingUser();

            assertThatThrownBy(() -> gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId))
                    .isInstanceOf(InvalidIdentifierStatusException.class);

            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.RELEASING);
        }

        @Test
        @DisplayName("其他用户对已占位的标识 reserve，抛出 IdentifierNotOwnedByUserException，占位仍属于原用户")
        void reserveReservedIdentifierByAnotherUserIsRejected() {
            ShardedId first = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, first);

            assertThatThrownBy(() -> gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, newUserId()))
                    .isInstanceOf(IdentifierNotOwnedByUserException.class);

            assertLookup(EMAIL_IDENTITY_IDENTIFIER, first, IdentityLookupStatus.RESERVED);
        }

        @Test
        @DisplayName("其他用户对已 ACTIVE 的标识 reserve，抛出 IdentifierNotOwnedByUserException（属主校验优先于状态校验）")
        void reserveActiveIdentifierByAnotherUserIsRejected() {
            ShardedId first = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, first);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, first);

            assertThatThrownBy(() -> gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, newUserId()))
                    .isInstanceOf(IdentifierNotOwnedByUserException.class);

            assertLookup(EMAIL_IDENTITY_IDENTIFIER, first, IdentityLookupStatus.ACTIVE);
        }
    }

    // =====================================================================
    // 1. confirm
    // =====================================================================
    @Nested
    @DisplayName("1. 确认占位 confirm")
    class Confirm {

        @Test
        @DisplayName("确认占位后，能按标识路由到对应 userId")
        void confirmedReservationRoutesToUser() {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);

            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);

            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).contains(userId);
        }

        @Test
        @DisplayName("同一用户对已 ACTIVE 的记录重复 confirm 是幂等的")
        void confirmIsIdempotent() {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);

            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);

            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).contains(userId);
        }

        @Test
        @DisplayName("没有 reserve 直接 confirm，抛出 IdentifierNotExistException")
        void confirmWithoutReservationFails() {
            assertThatThrownBy(() -> gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, newUserId()))
                    .isInstanceOf(IdentifierNotExistException.class);
            assertThat(count()).isZero();
        }

        @Test
        @DisplayName("对 RELEASING 的记录 confirm 被拒绝，状态保持 RELEASING")
        void confirmReleasingIdentifierIsRejected() {
            ShardedId userId = newReleasingUser();

            assertThatThrownBy(() -> gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId))
                    .isInstanceOf(InvalidIdentifierStatusException.class);

            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.RELEASING);
        }

        @Test
        @DisplayName("确认他人的占位被拒绝，占位保持 RESERVED 且仍属于原用户")
        void confirmReservationOfAnotherUserIsRejected() {
            ShardedId first = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, first);

            assertThatThrownBy(() -> gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, newUserId()))
                    .isInstanceOf(IdentifierNotOwnedByUserException.class);

            assertLookup(EMAIL_IDENTITY_IDENTIFIER, first, IdentityLookupStatus.RESERVED);
        }

        @Test
        @DisplayName("确认他人已 ACTIVE 的记录被拒绝，记录保持 ACTIVE 且仍属于原用户")
        void confirmActiveRecordOfAnotherUserIsRejected() {
            ShardedId first = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, first);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, first);

            assertThatThrownBy(() -> gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, newUserId()))
                    .isInstanceOf(IdentifierNotOwnedByUserException.class);

            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).contains(first);
        }
    }

    // =====================================================================
    // 2. insertConfirmed
    // =====================================================================
    @Nested
    @DisplayName("2. 直接插入已确认记录 insertConfirmed")
    class InsertConfirmed {

        @Test
        @Transactional
        @DisplayName("直接插入已确认记录后，能按标识路由到对应 userId")
        void insertConfirmedRoutesToUser() {
            ShardedId userId = newUserId();

            gateway.insertConfirmed(EMAIL_IDENTITY_IDENTIFIER, userId);

            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).contains(userId);
        }

        @Test
        @Transactional
        @DisplayName("同一标识重复 insertConfirmed 不幂等，抛出 IdentifierAlreadyTakenException")
        void insertConfirmedTwiceIsRejected() {
            ShardedId userId = newUserId();
            gateway.insertConfirmed(EMAIL_IDENTITY_IDENTIFIER, userId);

            assertThatThrownBy(() -> gateway.insertConfirmed(EMAIL_IDENTITY_IDENTIFIER, userId))
                    .isInstanceOf(IdentifierAlreadyTakenException.class);
        }

        @Test
        @DisplayName("标识已被他人 reserve（独立提交）时，insertConfirmed 被拒绝，占位不受影响")
        void insertConfirmedOnReservedIdentifierIsRejected() {
            ShardedId reservedBy = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, reservedBy);   // 独立提交

            assertThatThrownBy(() -> tx.executeWithoutResult(
                    s -> gateway.insertConfirmed(EMAIL_IDENTITY_IDENTIFIER, newUserId())))
                    .isInstanceOf(IdentifierAlreadyTakenException.class);

            assertLookup(EMAIL_IDENTITY_IDENTIFIER, reservedBy, IdentityLookupStatus.RESERVED);
        }

        @Test
        @DisplayName("标识已 ACTIVE 时，insertConfirmed 被拒绝")
        void insertConfirmedOnActiveIdentifierIsRejected() {
            ShardedId owner = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, owner);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, owner);

            assertThatThrownBy(() -> tx.executeWithoutResult(
                    s -> gateway.insertConfirmed(EMAIL_IDENTITY_IDENTIFIER, newUserId())))
                    .isInstanceOf(IdentifierAlreadyTakenException.class);

            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).contains(owner);
        }
    }

    // =====================================================================
    // 3. cancelReserve
    // =====================================================================
    @Nested
    @DisplayName("3. 取消占位 cancelReserve")
    class CancelReserve {

        @Test
        @DisplayName("取消 RESERVED 状态的占位记录会将其删除")
        void cancelReservedReservationDeletesIt() {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);

            gateway.cancelReserve(EMAIL_IDENTITY_IDENTIFIER, userId);

            assertThat(selectByIdentifier(EMAIL_IDENTITY_IDENTIFIER)).isNull();
            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).isEmpty();
        }

        @Test
        @DisplayName("重复取消是幂等的")
        void cancelReserveIsIdempotent() {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.cancelReserve(EMAIL_IDENTITY_IDENTIFIER, userId);

            gateway.cancelReserve(EMAIL_IDENTITY_IDENTIFIER, userId);

            assertThat(selectByIdentifier(EMAIL_IDENTITY_IDENTIFIER)).isNull();
            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).isEmpty();
        }

        @Test
        @DisplayName("取消不存在的占位是合法的（视为已取消）")
        void cancelNonexistentReservationIsAllowed() {
            gateway.cancelReserve(EMAIL_IDENTITY_IDENTIFIER, newUserId());

            assertThat(selectByIdentifier(EMAIL_IDENTITY_IDENTIFIER)).isNull();
            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).isEmpty();
        }

        @Test
        @DisplayName("标识已 ACTIVE 时，即使是同一用户也无法 cancelReserve，记录保持 ACTIVE")
        void cancelReserveOnActiveIdentifierIsRejectedEvenForSameUser() {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);

            assertThatThrownBy(() -> gateway.cancelReserve(EMAIL_IDENTITY_IDENTIFIER, userId))
                    .isInstanceOf(InvalidIdentifierStatusException.class);

            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).contains(userId);
        }

        @Test
        @DisplayName("标识 RELEASING 时，cancelReserve 被拒绝，记录保持 RELEASING")
        void cancelReserveOnReleasingIdentifierIsRejected() {
            ShardedId userId = newReleasingUser();

            assertThatThrownBy(() -> gateway.cancelReserve(EMAIL_IDENTITY_IDENTIFIER, userId))
                    .isInstanceOf(InvalidIdentifierStatusException.class);

            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.RELEASING);
        }

        @Test
        @DisplayName("他人已持有该标识时，cancelReserve 视为已完成，且不影响他人记录")
        void cancelReservationOfAnotherUserIsNoOp() {
            ShardedId first = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, first);

            gateway.cancelReserve(EMAIL_IDENTITY_IDENTIFIER, newUserId());

            assertLookup(EMAIL_IDENTITY_IDENTIFIER, first, IdentityLookupStatus.RESERVED);
        }

        @Test
        @DisplayName("取消他人已 ACTIVE 的记录是被允许的，记录保持 ACTIVE 且仍属于原用户")
        void cancelActiveRecordOfAnotherUserIsAllowed() {
            ShardedId first = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, first);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, first);

            gateway.cancelReserve(EMAIL_IDENTITY_IDENTIFIER, newUserId());


            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).contains(first);
        }
    }

    // =====================================================================
    // 4. beginRelease
    // =====================================================================
    @Nested
    @DisplayName("4. 开始释放 beginRelease")
    class BeginRelease {

        @Test
        @DisplayName("RELEASING 的记录不能路由到对应的 user")
        void releasingIdentityIsNotVisibleToLoginRouting() {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);

            gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, userId);

            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.RELEASING);
            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).isEmpty();
        }

        @Test
        @DisplayName("beginRelease 是幂等的")
        void beginReleaseIsIdempotent() {
            ShardedId userId = newReleasingUser();

            gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, userId);

            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.RELEASING);
            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).isEmpty();
        }

        @Test
        @DisplayName("beginRelease 一个未确认（RESERVED）的记录被拒绝，状态保持 RESERVED")
        void beginReleaseOnReservedRecordIsRejected() {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);

            assertThatThrownBy(() -> gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, userId))
                    .isInstanceOf(InvalidIdentifierStatusException.class);

            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.RESERVED);
        }

        @Test
        @DisplayName("beginRelease 一个不存在的记录被拒绝，抛出 IdentifierNotExistException")
        void beginReleaseOnNonexistentRecordIsRejected() {
            assertThatThrownBy(() -> gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, newUserId()))
                    .isInstanceOf(IdentifierNotExistException.class);
        }

        @Test
        @DisplayName("beginRelease 他人的 ACTIVE 记录被拒绝，记录保持 ACTIVE 且仍可路由到原用户")
        void beginReleaseOnRecordOfAnotherUserIsRejected() {
            ShardedId owner = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, owner);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, owner);

            assertThatThrownBy(() -> gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, newUserId()))
                    .isInstanceOf(IdentifierNotOwnedByUserException.class);

            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).contains(owner);
        }
    }

    // =====================================================================
    // 5. completeRelease
    // =====================================================================
    @Nested
    @DisplayName("5. 完成释放 completeRelease")
    class CompleteRelease {

        @Test
        @DisplayName("completeRelease 后记录被删除，且不能路由到对应的 user")
        void completedReleaseDeletesRecord() {
            ShardedId userId = newReleasingUser();

            gateway.completeRelease(EMAIL_IDENTITY_IDENTIFIER, userId);

            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).isEmpty();
            assertThat(selectByIdentifier(EMAIL_IDENTITY_IDENTIFIER)).isNull();
        }

        @Test
        @DisplayName("completeRelease 是幂等的")
        void completeReleaseIsIdempotent() {
            ShardedId userId = newReleasingUser();
            gateway.completeRelease(EMAIL_IDENTITY_IDENTIFIER, userId);

            gateway.completeRelease(EMAIL_IDENTITY_IDENTIFIER, userId);

            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).isEmpty();
            assertThat(selectByIdentifier(EMAIL_IDENTITY_IDENTIFIER)).isNull();
        }

        @Test
        @DisplayName("completeRelease 一个未确认（RESERVED）的记录被拒绝，状态保持 RESERVED")
        void completeReleaseOnReservedRecordIsRejected() {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);

            assertThatThrownBy(() -> gateway.completeRelease(EMAIL_IDENTITY_IDENTIFIER, userId))
                    .isInstanceOf(InvalidIdentifierStatusException.class);

            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.RESERVED);
        }

        @Test
        @DisplayName("没有 beginRelease 就 completeRelease 一个 ACTIVE 的记录被拒绝，防止直接删掉生效的标识")
        void completeReleaseOnActiveRecordIsRejected() {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);

            assertThatThrownBy(() -> gateway.completeRelease(EMAIL_IDENTITY_IDENTIFIER, userId))
                    .isInstanceOf(InvalidIdentifierStatusException.class);

            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).contains(userId);
        }

        @Test
        @DisplayName("他人已持有该标识时，completeReleas 视为已完成，且不影响他人记录")
        void completeReleaseOnRecordOfAnotherUserIsNoOp() {
            ShardedId owner = newReleasingUser();
            gateway.completeRelease(EMAIL_IDENTITY_IDENTIFIER, newUserId());

            assertLookup(EMAIL_IDENTITY_IDENTIFIER, owner, IdentityLookupStatus.RELEASING);
        }
    }

    // =====================================================================
    // 6. abortRelease
    // =====================================================================
    @Nested
    @DisplayName("6. 回滚释放 abortRelease")
    class AbortRelease {

        @Test
        @DisplayName("abortRelease 后，记录恢复 ACTIVE，可以查到并正常路由到用户")
        void abortedReleaseRestoresActiveAndRouting() {
            ShardedId userId = newReleasingUser();

            gateway.abortRelease(EMAIL_IDENTITY_IDENTIFIER, userId);

            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.ACTIVE);
            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).contains(userId);
        }

        @Test
        @DisplayName("abortRelease 是幂等的")
        void abortReleaseIsIdempotent() {
            ShardedId userId = newReleasingUser();
            gateway.abortRelease(EMAIL_IDENTITY_IDENTIFIER, userId);

            gateway.abortRelease(EMAIL_IDENTITY_IDENTIFIER, userId);

            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).contains(userId);
            assertThat(selectByIdentifier(EMAIL_IDENTITY_IDENTIFIER)).isNotNull();
        }

        @Test
        @DisplayName("abortRelease 一个未确认（RESERVED）的记录被拒绝，状态保持 RESERVED")
        void abortReleaseOnReservedRecordIsRejected() {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);

            assertThatThrownBy(() -> gateway.abortRelease(EMAIL_IDENTITY_IDENTIFIER, userId))
                    .isInstanceOf(InvalidIdentifierStatusException.class);

            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.RESERVED);
        }

        @Test
        @DisplayName("abortRelease 一个不存在的记录被拒绝，抛出 IdentifierNotExistException")
        void abortReleaseOnNonexistentRecordIsRejected() {
            assertThatThrownBy(() -> gateway.abortRelease(EMAIL_IDENTITY_IDENTIFIER, newUserId()))
                    .isInstanceOf(IdentifierNotExistException.class);
        }

        @Test
        @DisplayName("abortRelease 他人处于 RELEASING 的记录被拒绝，记录保持 RELEASING 且仍属于原用户")
        void abortReleaseOnReleasingRecordOfAnotherUserIsRejected() {
            ShardedId owner = newReleasingUser();

            assertThatThrownBy(() -> gateway.abortRelease(EMAIL_IDENTITY_IDENTIFIER, newUserId()))
                    .isInstanceOf(IdentifierNotOwnedByUserException.class);

            assertLookup(EMAIL_IDENTITY_IDENTIFIER, owner, IdentityLookupStatus.RELEASING);
        }
    }

    // =====================================================================
    // 7. 标识被释放后重新被其他用户使用
    // =====================================================================
    @Nested
    @DisplayName("7. 标识释放后被其他用户重新使用")
    class ReuseAfterRelease {

        @Test
        @DisplayName("cancelReserve 后，标识可以被新用户重新 reserve 并 confirm")
        void identifierCanBeReservedAgainAfterCancelReserve() {
            ShardedId first = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, first);
            gateway.cancelReserve(EMAIL_IDENTITY_IDENTIFIER, first);

            ShardedId second = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, second);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, second);

            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).contains(second);
        }

        @Test
        @DisplayName("completeRelease 后，标识可以被新用户重新 reserve 并 confirm")
        void identifierCanBeReservedAgainAfterCompleteRelease() {
            ShardedId first = newReleasingUser();
            gateway.completeRelease(EMAIL_IDENTITY_IDENTIFIER, first);

            ShardedId second = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, second);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, second);

            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).contains(second);
        }

        @Test
        @DisplayName("不同标识互不影响：a 的状态变化不影响 b")
        void differentIdentifiersAreIndependent() {
            ShardedId userA = newUserId();
            ShardedId userB = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userA);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userA);
            gateway.reserve(ANOTHER_EMAIL_IDENTIFIER, userB);

            gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, userA);

            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userA, IdentityLookupStatus.RELEASING);
            assertLookup(ANOTHER_EMAIL_IDENTIFIER, userB, IdentityLookupStatus.RESERVED);
        }
    }

    // =====================================================================
    // 8. reclaimReservedStale
    // =====================================================================
    @Nested
    @DisplayName("8. 回收遗留的 RESERVED")
    class ReclaimReserved {

        @Test
        @DisplayName("过期的 RESERVED 记录在身份行不存在时，被 reclaimReservedStale 删除")
        void reclaimDeletesStaleReservedWhenIdentityRowMissing() {
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, newUserId());
            backdate(EMAIL_IDENTITY_IDENTIFIER);

            int handled = gateway.reclaimReservedStale(Duration.ofMinutes(30), 100, pending -> false);

            assertThat(handled).isEqualTo(1);
            assertThat(count()).isZero();
        }

        @Test
        @DisplayName("过期的 RESERVED 记录在身份行已存在时，被 reclaimReservedStale 提升为 ACTIVE")
        void reclaimPromotesStaleReservedWhenIdentityRowExists() {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            backdate(EMAIL_IDENTITY_IDENTIFIER);

            int handled = gateway.reclaimReservedStale(Duration.ofMinutes(30), 100, pending -> true);

            assertThat(handled).isEqualTo(1);
            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.ACTIVE);
            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).contains(userId);
        }

        @Test
        @DisplayName("reclaimReservedStale 忽略未过期的 RESERVED 和所有 ACTIVE 记录")
        void reclaimIgnoresRecentReservedAndActive() {
            ShardedId activeUser = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, activeUser);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, activeUser);
            backdate(EMAIL_IDENTITY_IDENTIFIER);                         // 过期但是 ACTIVE
            gateway.reserve(ANOTHER_EMAIL_IDENTIFIER, newUserId());      // RESERVED 但未过期

            int handled = gateway.reclaimReservedStale(Duration.ofMinutes(30), 100, pending -> false);

            assertThat(handled).isZero();
            assertThat(count()).isEqualTo(2);
        }

        @Test
        @DisplayName("reclaimReservedStale 不处理过期的 RELEASING 记录")
        void reclaimReservedIgnoresStaleReleasing() {
            ShardedId releasingUser = newReleasingUser();
            backdate(EMAIL_IDENTITY_IDENTIFIER);

            int handled = gateway.reclaimReservedStale(Duration.ofMinutes(30), 100, pending -> false);

            assertThat(handled).isZero();
            assertLookup(EMAIL_IDENTITY_IDENTIFIER, releasingUser, IdentityLookupStatus.RELEASING);
        }

        @Test
        @DisplayName("limit 生效：2 条过期记录、limit=1 时只处理 1 条")
        void reclaimRespectsLimit() {
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, newUserId());
            gateway.reserve(ANOTHER_EMAIL_IDENTIFIER, newUserId());
            backdateAll();

            int handled = gateway.reclaimReservedStale(Duration.ofMinutes(30), 1, pending -> false);

            assertThat(handled).isEqualTo(1);
            assertThat(count()).isEqualTo(1);
        }

        @Test
        @DisplayName("传给 identityExists 的 PendingLookup 携带正确的标识和 userId")
        void reclaimPassesCorrectPendingLookupToPredicate() {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            backdate(EMAIL_IDENTITY_IDENTIFIER);
            List<IdentityLookupGateway.PendingLookup> seen = new ArrayList<>();

            gateway.reclaimReservedStale(Duration.ofMinutes(30), 100, pending -> {
                seen.add(pending);
                return false;
            });

            assertThat(seen).hasSize(1);
            assertThat(seen.get(0).identifier().type()).isEqualTo(EMAIL_IDENTITY_IDENTIFIER.type());
            assertThat(seen.get(0).identifier().value()).isEqualTo(EMAIL_IDENTITY_IDENTIFIER.value());
            assertThat(seen.get(0).userId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("分片广播：多个标识分布在不同分片时，过期的 RESERVED 全部被扫到")
        void reclaimScansAllShards() {
            int total = 30;   // 数量足够大，哈希后几乎必然落到多个分片
            for (int i = 0; i < total; i++) {
                gateway.reserve(EmailIdentifier.from("shard" + i + "@example.com"), newUserId());
            }
            backdateAll();

            int handled = gateway.reclaimReservedStale(Duration.ofMinutes(30), 1000, pending -> false);

            assertThat(handled).isEqualTo(total);
            assertThat(count()).isZero();
        }
        @Test
        @DisplayName("某一行的 identityExists 抛异常，不影响其他过期记录被回收")
        void reclaimContinuesAfterPredicateThrowsForOneRow() {
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, newUserId());
            gateway.reserve(ANOTHER_EMAIL_IDENTIFIER, newUserId());
            backdateAll();

            int handled = gateway.reclaimReservedStale(Duration.ofMinutes(30), 100, pending -> {
                if (pending.identifier().value().equals(EMAIL_IDENTITY_IDENTIFIER.value())) {
                    throw new IllegalStateException("模拟查询超时");
                }
                return false;
            });

            assertThat(handled).isEqualTo(1);
            assertThat(count()).isEqualTo(1);                                   // 抛异常的那条保留
            assertThat(selectByIdentifier(EMAIL_IDENTITY_IDENTIFIER)).isNotNull();
        }
    }

    // =====================================================================
    // 9. reclaimReleasingStale
    // =====================================================================
    @Nested
    @DisplayName("9. 回收遗留的 RELEASING")
    class ReclaimReleasing {

        @Test
        @DisplayName("过期的 RELEASING 记录在身份行不存在时，被 reclaimReleasingStale 删除（完成释放）")
        void reclaimCompletesReleaseWhenIdentityRowMissing() {
            newReleasingUser();
            backdate(EMAIL_IDENTITY_IDENTIFIER);

            int handled = gateway.reclaimReleasingStale(Duration.ofMinutes(30), 100, pending -> false);

            assertThat(handled).isEqualTo(1);
            assertThat(count()).isZero();
        }

        @Test
        @DisplayName("过期的 RELEASING 记录在身份行仍存在时，被 reclaimReleasingStale 还原为 ACTIVE")
        void reclaimRestoresActiveWhenIdentityRowExists() {
            ShardedId userId = newReleasingUser();
            backdate(EMAIL_IDENTITY_IDENTIFIER);

            int handled = gateway.reclaimReleasingStale(Duration.ofMinutes(30), 100, pending -> true);

            assertThat(handled).isEqualTo(1);
            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.ACTIVE);
            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).contains(userId);
        }

        @Test
        @DisplayName("reclaimReleasingStale 忽略未过期的 RELEASING 和所有 ACTIVE 记录")
        void reclaimIgnoresRecentReleasingAndActive() {
            ShardedId activeUser = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, activeUser);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, activeUser);
            backdate(EMAIL_IDENTITY_IDENTIFIER);                         // 过期但是 ACTIVE

            ShardedId recentUser = newUserId();
            gateway.reserve(ANOTHER_EMAIL_IDENTIFIER, recentUser);
            gateway.confirm(ANOTHER_EMAIL_IDENTIFIER, recentUser);
            gateway.beginRelease(ANOTHER_EMAIL_IDENTIFIER, recentUser);  // RELEASING 但未过期

            int handled = gateway.reclaimReleasingStale(Duration.ofMinutes(30), 100, pending -> false);

            assertThat(handled).isZero();
            assertThat(count()).isEqualTo(2);
        }

        @Test
        @DisplayName("reclaimReleasingStale 不处理过期的 RESERVED 记录")
        void reclaimReleasingIgnoresStaleReserved() {
            ShardedId reservedUser = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, reservedUser);
            backdate(EMAIL_IDENTITY_IDENTIFIER);

            int handled = gateway.reclaimReleasingStale(Duration.ofMinutes(30), 100, pending -> false);

            assertThat(handled).isZero();
            assertLookup(EMAIL_IDENTITY_IDENTIFIER, reservedUser, IdentityLookupStatus.RESERVED);
        }

        @Test
        @DisplayName("limit 生效：2 条过期记录、limit=1 时只处理 1 条")
        void reclaimRespectsLimit() {
            newReleasingUser();
            ShardedId second = newUserId();
            gateway.reserve(ANOTHER_EMAIL_IDENTIFIER, second);
            gateway.confirm(ANOTHER_EMAIL_IDENTIFIER, second);
            gateway.beginRelease(ANOTHER_EMAIL_IDENTIFIER, second);
            backdateAll();

            int handled = gateway.reclaimReleasingStale(Duration.ofMinutes(30), 1, pending -> false);

            assertThat(handled).isEqualTo(1);
            assertThat(count()).isEqualTo(1);
        }
        @Test
        @DisplayName("某一行的 identityExists 抛异常，不影响其他过期记录被回收")
        void reclaimContinuesAfterPredicateThrowsForOneRow() {
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, newUserId());
            gateway.reserve(ANOTHER_EMAIL_IDENTIFIER, newUserId());
            backdateAll();

            int handled = gateway.reclaimReleasingStale(Duration.ofMinutes(30), 100, pending -> {
                if (pending.identifier().value().equals(EMAIL_IDENTITY_IDENTIFIER.value())) {
                    throw new IllegalStateException("模拟查询超时");
                }
                return false;
            });

            assertThat(handled).isEqualTo(1);
            assertThat(count()).isEqualTo(1);                                   // 抛异常的那条保留
            assertThat(selectByIdentifier(EMAIL_IDENTITY_IDENTIFIER)).isNotNull();
        }
    }

    // =====================================================================
    // 10. updated_at 刷新（回收依赖它判断"是否过期"）
    // =====================================================================
    @Nested
    @DisplayName("10. 状态变更必须刷新 updated_at")
    class UpdatedAtRefresh {

        @Test
        @DisplayName("很久以前 confirm 的记录刚 beginRelease，不应立刻被 reclaimReleasingStale 当作过期记录处理")
        void beginReleaseRefreshesUpdatedAt() {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);
            backdate(EMAIL_IDENTITY_IDENTIFIER);          // 模拟：这条 ACTIVE 记录是一小时前就生效的

            gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, userId);
            int handled = gateway.reclaimReleasingStale(Duration.ofMinutes(30), 100, pending -> false);

            assertThat(handled).isZero();
            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.RELEASING);
        }
        @Test
        @DisplayName("记录已经是 RELEASING 时重复 beginRelease（重试），应刷新 updated_at，避免释放进行中被回收")
        void idempotentBeginReleaseRefreshesUpdatedAt() {
            ShardedId userId = newReleasingUser();
            backdate(EMAIL_IDENTITY_IDENTIFIER);

            gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, userId);
            int handled = gateway.reclaimReleasingStale(Duration.ofMinutes(30), 100, pending -> false);

            assertThat(handled).isZero();
            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.RELEASING);
        }
        @Test
        @DisplayName("abortRelease 后再次 beginRelease，应重新计时而不是沿用旧的 updated_at")
        void secondBeginReleaseRefreshesUpdatedAt() {
            ShardedId userId = newReleasingUser();
            gateway.abortRelease(EMAIL_IDENTITY_IDENTIFIER, userId);
            backdate(EMAIL_IDENTITY_IDENTIFIER);

            gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, userId);
            int handled = gateway.reclaimReleasingStale(Duration.ofMinutes(30), 100, pending -> false);

            assertThat(handled).isZero();
            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.RELEASING);
        }

        @Test
        @DisplayName("同一用户在 RESERVED 期间重复 reserve（重试），应刷新 updated_at，避免被回收")
        void idempotentReserveRefreshesUpdatedAt() {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            backdate(EMAIL_IDENTITY_IDENTIFIER);

            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            int handled = gateway.reclaimReservedStale(Duration.ofMinutes(30), 100, pending -> false);

            assertThat(handled).isZero();
            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.RESERVED);
        }
    }

    // =====================================================================
    // 11. 事务边界
    // =====================================================================
    @Nested
    @DisplayName("11. 事务边界（REQUIRES_NEW / MANDATORY）")
    class TransactionBoundary {

        @Test
        @DisplayName("reserve 独立提交：外层事务回滚，占位仍然保留")
        void reserveSurvivesOuterRollback() {
            ShardedId userId = newUserId();

            tx.executeWithoutResult(status -> {
                gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
                status.setRollbackOnly();
            });

            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.RESERVED);
        }

        @Test
        @DisplayName("confirm 独立提交：外层事务回滚，ACTIVE 仍然保留")
        void confirmSurvivesOuterRollback() {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);

            tx.executeWithoutResult(status -> {
                gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);
                status.setRollbackOnly();
            });

            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).contains(userId);
        }

        @Test
        @DisplayName("beginRelease 独立提交：外层事务回滚，RELEASING 仍然保留")
        void beginReleaseSurvivesOuterRollback() {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);

            tx.executeWithoutResult(status -> {
                gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, userId);
                status.setRollbackOnly();
            });

            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.RELEASING);
        }

        @Test
        @DisplayName("reserve 在外层事务内抛异常导致回滚时，占位仍然保留（交给回收任务/补偿处理）")
        void reserveSurvivesOuterExceptionRollback() {
            ShardedId userId = newUserId();

            assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
                gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
                throw new IllegalStateException("模拟聚合落库失败");
            })).isInstanceOf(IllegalStateException.class);

            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.RESERVED);
        }

        @Test
        @DisplayName("insertConfirmed 没有外层事务时，抛出 IllegalTransactionStateException（MANDATORY）")
        void insertConfirmedWithoutTransactionIsRejected() {
            assertThatThrownBy(() -> gateway.insertConfirmed(EMAIL_IDENTITY_IDENTIFIER, newUserId()))
                    .isInstanceOf(IllegalTransactionStateException.class);

            assertThat(count()).isZero();
        }

        @Test
        @DisplayName("insertConfirmed 随外层事务提交：提交后可路由")
        void insertConfirmedIsVisibleAfterOuterCommit() {
            ShardedId userId = newUserId();

            tx.executeWithoutResult(status -> gateway.insertConfirmed(EMAIL_IDENTITY_IDENTIFIER, userId));

            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).contains(userId);
        }

        @Test
        @DisplayName("insertConfirmed 随外层事务回滚：回滚后记录不存在")
        void insertConfirmedIsRolledBackWithOuterTransaction() {
            ShardedId userId = newUserId();

            tx.executeWithoutResult(status -> {
                gateway.insertConfirmed(EMAIL_IDENTITY_IDENTIFIER, userId);
                status.setRollbackOnly();
            });

            assertThat(selectByIdentifier(EMAIL_IDENTITY_IDENTIFIER)).isNull();
            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).isEmpty();
        }
    }

    // =====================================================================
    // 12. 并发
    // =====================================================================
    @Nested
    @DisplayName("12. 并发")
    class Concurrency {

        private static final int THREADS = 8;

        @RepeatedTest(5)
        @DisplayName("不同用户并发 reserve 同一标识：恰好 1 个成功，其余被拒绝，最终占位属于成功者")
        void concurrentReserveByDifferentUsersHasExactlyOneWinner() throws Exception {
            ConcurrentLinkedQueue<ShardedId> winners = new ConcurrentLinkedQueue<>();
            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                ShardedId userId = newUserId();
                tasks.add(() -> {
                    gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
                    winners.add(userId);
                });
            }

            List<Throwable> failures = runConcurrently(tasks);

            assertThat(winners).hasSize(1);
            assertThat(failures).hasSize(THREADS - 1);
            assertThat(failures).allSatisfy(t -> assertThat(t).isInstanceOfAny(
                    IdentifierNotOwnedByUserException.class, IdentifierAlreadyTakenException.class));
            assertThat(count()).isEqualTo(1);
            assertLookup(EMAIL_IDENTITY_IDENTIFIER, winners.peek(), IdentityLookupStatus.RESERVED);
        }

        @RepeatedTest(5)
        @DisplayName("同一用户并发重复 reserve（重试风暴）：全部幂等成功，只有 1 条记录")
        void concurrentReserveBySameUserAllSucceed() throws Exception {
            ShardedId userId = newUserId();
            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                tasks.add(() -> gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId));
            }

            List<Throwable> failures = runConcurrently(tasks);

            assertThat(failures).isEmpty();
            assertThat(count()).isEqualTo(1);
            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.RESERVED);
        }

        @RepeatedTest(5)
        @DisplayName("同一用户并发重复 confirm：全部幂等成功，最终 ACTIVE")
        void concurrentConfirmBySameUserAllSucceed() throws Exception {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                tasks.add(() -> gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId));
            }

            List<Throwable> failures = runConcurrently(tasks);

            assertThat(failures).isEmpty();
            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.ACTIVE);
        }

        @RepeatedTest(5)
        @DisplayName("同一用户并发重复 beginRelease：全部幂等成功，最终 RELEASING")
        void concurrentBeginReleaseBySameUserAllSucceed() throws Exception {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);
            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                tasks.add(() -> gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, userId));
            }

            List<Throwable> failures = runConcurrently(tasks);

            assertThat(failures).isEmpty();
            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.RELEASING);
        }

        @RepeatedTest(5)
        @DisplayName("同一用户并发重复 cancelReserve：全部幂等成功，记录被删除")
        void concurrentCancelReserveBySameUserAllSucceed() throws Exception {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                tasks.add(() -> gateway.cancelReserve(EMAIL_IDENTITY_IDENTIFIER, userId));
            }

            List<Throwable> failures = runConcurrently(tasks);

            assertThat(failures).isEmpty();
            assertThat(count()).isZero();
        }

        @RepeatedTest(10)
        @DisplayName("confirm 与回收任务（补确认）并发处理同一条过期 RESERVED：都不报错，最终必为 ACTIVE")
        void concurrentConfirmAndReclaimPromoteEndsActive() throws Exception {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            backdate(EMAIL_IDENTITY_IDENTIFIER);

            List<Runnable> tasks = List.of(
                    () -> gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId),
                    () -> gateway.reclaimReservedStale(Duration.ofMinutes(30), 100, pending -> true));

            List<Throwable> failures = runConcurrently(tasks);

            assertThat(failures).isEmpty();
            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.ACTIVE);
            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).contains(userId);
        }

        @RepeatedTest(10)
        @DisplayName("completeRelease 与回收任务（完成释放）并发处理同一条过期 RELEASING：都不报错，记录最终被删除")
        void concurrentCompleteReleaseAndReclaimEndsDeleted() throws Exception {
            ShardedId userId = newReleasingUser();
            backdate(EMAIL_IDENTITY_IDENTIFIER);

            List<Runnable> tasks = List.of(
                    () -> gateway.completeRelease(EMAIL_IDENTITY_IDENTIFIER, userId),
                    () -> gateway.reclaimReleasingStale(Duration.ofMinutes(30), 100, pending -> false));

            List<Throwable> failures = runConcurrently(tasks);

            assertThat(failures).isEmpty();
            assertThat(count()).isZero();
        }

        /**
         * 同时放行所有任务并收集各自抛出的异常（成功的任务不产生记录）。
         * 每个任务占用一个独立连接，线程数不要超过连接池大小。
         */
        private List<Throwable> runConcurrently(List<Runnable> tasks) throws Exception {
            ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
            try {
                CountDownLatch ready = new CountDownLatch(tasks.size());
                CountDownLatch go = new CountDownLatch(1);
                List<Future<Throwable>> futures = new ArrayList<>();
                for (Runnable task : tasks) {
                    Callable<Throwable> wrapped = () -> {
                        ready.countDown();
                        go.await();
                        try {
                            task.run();
                            return null;
                        } catch (Throwable t) {
                            return t;
                        }
                    };
                    futures.add(pool.submit(wrapped));
                }
                ready.await();
                go.countDown();

                List<Throwable> failures = new ArrayList<>();
                for (Future<Throwable> future : futures) {
                    Throwable t = future.get(30, TimeUnit.SECONDS);
                    if (t != null) {
                        failures.add(t);
                    }
                }
                return failures;
            } finally {
                pool.shutdownNow();
            }
        }
    }

    // =====================================================================
    // 13. 回收过程中数据被改动（ABA）
    // =====================================================================
    /**
     * reclaim 的流程是：selectStale（选出过期记录）→ identityExists.test(...) → 带条件的 UPDATE/DELETE。
     * identityExists 恰好在"选出"和"修改"之间执行，所以在这个回调里改数据，
     * 就能确定性地复现"选出之后、修改之前记录被别人动过"的窗口，不需要线程和 sleep。
     * 这组用例依赖 reclaim 的 UPDATE/DELETE 带上"updated_at 仍早于阈值"的条件；条件没加之前，ABA 用例会失败。
     */
    @Nested
    @DisplayName("13. 回收过程中数据被改动（ABA 防护）")
    class ReclaimRace {

        @Test
        @DisplayName("回收 RESERVED：判断期间同一用户重试 reserve（刷新了时间），占位不能被删除")
        void reclaimDoesNotDeleteReservedThatWasRefreshedDuringCheck() {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            backdate(EMAIL_IDENTITY_IDENTIFIER);

            int handled = gateway.reclaimReservedStale(Duration.ofMinutes(30), 100, pending -> {
                gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, pending.userId());   // 用户重试，刷新 updated_at
                return false;                                                   // 按旧判断，会走"删除占位"
            });

            assertThat(handled).isZero();
            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.RESERVED);
        }

        @Test
        @DisplayName("回收 RELEASING：判断期间同一用户 abortRelease 后又 beginRelease（状态和属主都没变，只有时间变了），记录不能被删除")
        void reclaimDoesNotDeleteReleasingThatWasReReleasedDuringCheck() {
            ShardedId userId = newReleasingUser();
            backdate(EMAIL_IDENTITY_IDENTIFIER);

            int handled = gateway.reclaimReleasingStale(Duration.ofMinutes(30), 100, pending -> {
                gateway.abortRelease(EMAIL_IDENTITY_IDENTIFIER, pending.userId());
                gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, pending.userId());   // RELEASING → ACTIVE → RELEASING
                return false;
            });

            assertThat(handled).isZero();
            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.RELEASING);
        }

        @Test
        @DisplayName("回收 RELEASING：判断期间又开始了新一轮释放，不能被还原为 ACTIVE（否则会打断正在进行的释放）")
        void reclaimDoesNotRestoreReleasingThatWasReReleasedDuringCheck() {
            ShardedId userId = newReleasingUser();
            backdate(EMAIL_IDENTITY_IDENTIFIER);

            int handled = gateway.reclaimReleasingStale(Duration.ofMinutes(30), 100, pending -> {
                gateway.abortRelease(EMAIL_IDENTITY_IDENTIFIER, pending.userId());
                gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, pending.userId());
                return true;                                                         // 按旧判断，会走"还原为 ACTIVE"
            });

            assertThat(handled).isZero();
            assertLookup(EMAIL_IDENTITY_IDENTIFIER, userId, IdentityLookupStatus.RELEASING);
        }

        @Test
        @DisplayName("回收 RESERVED：判断期间正常流程已 confirm，ACTIVE 记录不能被删除（状态条件兜底）")
        void reclaimDoesNotDeleteReservedThatWasConfirmedDuringCheck() {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            backdate(EMAIL_IDENTITY_IDENTIFIER);

            int handled = gateway.reclaimReservedStale(Duration.ofMinutes(30), 100, pending -> {
                gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, pending.userId());
                return false;
            });

            assertThat(handled).isZero();
            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).contains(userId);
        }

        @Test
        @DisplayName("回收 RELEASING：判断期间正常流程已 abortRelease，ACTIVE 记录不能被删除（状态条件兜底）")
        void reclaimDoesNotDeleteReleasingThatWasAbortedDuringCheck() {
            ShardedId userId = newReleasingUser();
            backdate(EMAIL_IDENTITY_IDENTIFIER);

            int handled = gateway.reclaimReleasingStale(Duration.ofMinutes(30), 100, pending -> {
                gateway.abortRelease(EMAIL_IDENTITY_IDENTIFIER, pending.userId());
                return false;
            });

            assertThat(handled).isZero();
            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).contains(userId);
        }
    }

    // =====================================================================
    // 辅助方法
    // =====================================================================

    private IdentityLookupPO selectByIdentifier(IdentityIdentifier identifier) {
        return mapper.selectByIdentifier(identifier.type().getCode(), identifier.value());
    }

    /** 断言记录存在、属于指定用户、处于指定状态（用于拒绝类用例证明"状态未被改动"） */
    private void assertLookup(IdentityIdentifier identifier, ShardedId owner, IdentityLookupStatus status) {
        IdentityLookupPO po = selectByIdentifier(identifier);
        assertThat(po).isNotNull();
        assertThat(po.getUserId()).isEqualTo(owner.value());
        assertThat(po.getStatus()).isEqualTo(status.getCode());
    }

    /** 造一条处于 RELEASING 的 EMAIL_IDENTITY_IDENTIFIER 记录，返回其属主 */
    private ShardedId newReleasingUser() {
        ShardedId userId = newUserId();
        gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
        gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);
        gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, userId);
        return userId;
    }

    /** 把指定标识的 updated_at 改到一小时前，模拟"崩溃遗留已久" */
    private void backdate(IdentityIdentifier identifier) {
        jdbc.update(
                "UPDATE identity_lookup SET updated_at = DATE_SUB(NOW(3), INTERVAL 1 HOUR) "
                        + "WHERE identity_type = ? AND identifier = ?",
                identifier.type().getCode(), identifier.value());
    }

    /** 把所有记录的 updated_at 改到一小时前（广播到所有分片） */
    private void backdateAll() {
        jdbc.update("UPDATE identity_lookup SET updated_at = DATE_SUB(NOW(3), INTERVAL 1 HOUR)");
    }

    private int count() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM identity_lookup", Integer.class);
        return n == null ? 0 : n;
    }

    private ShardedId newUserId() {
        return idGenerator.nextId(IdGenerationCommand.builder("auth_test_user").build());
    }
}