package com.ovelin.mall.auth.infrastructure.persistence.gateway;

import com.ovelin.mall.auth.domain.exception.IdentifierAlreadyTakenException;
import com.ovelin.mall.auth.domain.exception.InvalidIdentifierStatusException;
import com.ovelin.mall.auth.domain.exception.IdentifierNotExistException;
import com.ovelin.mall.auth.domain.exception.IdentifierNotOwnedByUserException;
import com.ovelin.mall.auth.domain.model.enums.IdentityLookupStatus;
import com.ovelin.mall.auth.domain.model.identityIdentifier.EmailIdentifier;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifier;
import com.ovelin.mall.auth.infrastructure.persistence.mapper.IdentityLookupMapper;
import com.ovelin.mall.auth.infrastructure.persistence.po.IdentityLookupPO;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.id.generator.api.IdGenerationCommand;
import com.ovelin.mall.id.generator.api.IdGenerator;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class IdentityLookupGatewayIT {

    private static final IdentityIdentifier EMAIL_IDENTITY_IDENTIFIER =
            EmailIdentifier.from("a@example.com");

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

    private IdentityLookupPO selectByIdentifier(IdentityIdentifier identifier){
        return mapper.selectByIdentifier(identifier.type().getCode(), identifier.value());
    }
    @Nested
    @DisplayName("0, 申请占位")
    @Order(Order.DEFAULT)
    class Reserve{
        @Test
        @DisplayName("PENDING 状态的占位记录对登录路由不可见")
        void pendingReservationIsNotVisibleToLoginRouting() {
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, newUserId());
            assertThat(selectByIdentifier(EMAIL_IDENTITY_IDENTIFIER)).isNotNull();
            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).isEmpty();
        }
        @Test
        @DisplayName("同一用户对同一标识在 PENDING 期间重复 reserve，视为幂等成功")
        void reserveIsIdempotentForSameUserWhilePending() {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);

            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            assertThat(selectByIdentifier(EMAIL_IDENTITY_IDENTIFIER)).isNotNull();
            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).isEmpty();
        }

        // 该测试涉及下一个状态，可以看完下个状态再回来看
        @Test
        @DisplayName("标识已激活不可重新占位")
        void reserveAnConfirmedIdentifierIsReject() {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);

            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);

            assertThatThrownBy(()->gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId)).isInstanceOf(InvalidIdentifierStatusException.class);

        }
        @Test
        @DisplayName("释放中不可重新占位")
        void reserveAnReleasingIdentifierIsReject() {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);

            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);

            gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, userId);

            assertThatThrownBy(()->gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId)).isInstanceOf(InvalidIdentifierStatusException.class);

        }

        @Test
        @DisplayName("其他用户对已占位的标识发起 reserve 时，抛出 IdentifierNotOwnedByUserException")
        void reserveByAnotherUserIsRejected() {
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, newUserId());

            assertThatThrownBy(() -> gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, newUserId()))
                    .isInstanceOf(IdentifierNotOwnedByUserException.class);
        }
    }
   @Nested
   @DisplayName("1. 确认占位")
   @Order(Order.DEFAULT + 1)
   class Confirm{
       @Test
       @DisplayName("确认占位后，能按标识路由到对应 userId")
       void confirmedReservationRoutesToUser() {
           ShardedId userId = newUserId();
           gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
           gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);
           assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).contains(userId);
       }

       @Test
       @DisplayName("confirm 对同一用户的reserve重复confirm是幂等的")
       void confirmIsIdempotent() {
           ShardedId userId = newUserId();
           gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
           gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);

           gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);

           assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).contains(userId);
       }


       @Test
       @DisplayName("没有 reserve 直接 confirm，抛出 IdentifierNotExistException （合法状态的前一个状态）")
       void confirmWithoutReservationFails() {
           assertThatThrownBy(() -> gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, newUserId()))
                   .isInstanceOf(IdentifierNotExistException.class);
       }

       @Test
       @DisplayName("confirm 对已经开始释放的记录确认是拒绝的")
       void confirmReleasingIdentifierIsReject() {
           ShardedId userId = newUserId();
           gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
           gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);
           gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, userId);

           assertThatThrownBy(()->  gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId)).isInstanceOf(InvalidIdentifierStatusException.class);
       }

       @Test
       @DisplayName("确认他人的占位是被拒绝的")
       void confirmReservationOfAnotherUserIsReject() {
           ShardedId first = newUserId();
           gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, first);

           ShardedId second = newUserId();

           assertThatThrownBy(()-> gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, second)).isInstanceOf(IdentifierNotOwnedByUserException.class);
          ;
       }

    }

    @Nested
    @DisplayName("2. 直接插入已确认记录")
    @Order(Order.DEFAULT + 2)
    class InsertConfirmed{
        @Test
        @DisplayName("直接插入确认占位后，能按标识路由到对应 userId")
        @Transactional
        void insertConfirmedRoutesToUser() {
            ShardedId userId = newUserId();
            gateway.insertConfirmed(EMAIL_IDENTITY_IDENTIFIER, userId);
            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).contains(userId);
        }
        @Test
        @DisplayName("重复插入ACTIVE操作是不幂等的")
        @Transactional
        void insertConfirmedIsRejected() {
            ShardedId userId = newUserId();
            gateway.insertConfirmed(EMAIL_IDENTITY_IDENTIFIER, userId);
            assertThatThrownBy(()-> gateway.insertConfirmed(EMAIL_IDENTITY_IDENTIFIER, userId)).isInstanceOf(IdentifierAlreadyTakenException.class);
        }
        @Test
        @DisplayName("插入已被抢占的身份是被拒绝的")
        void insertConfirmedReservingIsReject() {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);   // 独立提交

            assertThatThrownBy(() -> tx.executeWithoutResult(
                    s -> gateway.insertConfirmed(EMAIL_IDENTITY_IDENTIFIER, userId)))
                    .isInstanceOf(IdentifierAlreadyTakenException.class);
        }
    }
    @Nested
    @DisplayName("3. 取消占位")
    @Order(Order.DEFAULT + 3)
    class CancelReserve{

        @Test
        @DisplayName("取消PENDING 状态的占位记录对会将其删除")
        void cancelPendingReservationMustDeleteIt() {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.cancelReserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            assertThat(selectByIdentifier(EMAIL_IDENTITY_IDENTIFIER)).isNull();
            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).isEmpty();
        }
        @Test
        @DisplayName("重复执行是幂等的")
        void cancelPendingReservationIsIdempotent() {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.cancelReserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.cancelReserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            assertThat(selectByIdentifier(EMAIL_IDENTITY_IDENTIFIER)).isNull();
            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).isEmpty();
        }

        @Test
        @DisplayName("取消不存在的是合法的")
        void cancelNotExistReservationIsIdempotent() {
            ShardedId userId = newUserId();
            gateway.cancelReserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            assertThat(selectByIdentifier(EMAIL_IDENTITY_IDENTIFIER)).isNull();
            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).isEmpty();
        }

        @Test
        @DisplayName("标识已 ACTIVE 时，即使是同一用户也无法取消")
        void reserveAfterConfirmIsRejectedEvenForSameUser() {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);

            assertThatThrownBy(() -> gateway.cancelReserve(EMAIL_IDENTITY_IDENTIFIER, userId))
                    .isInstanceOf(InvalidIdentifierStatusException.class);
        }


        @Test
        @DisplayName("取消他人的占位是被拒绝的")
        void cancelReservationOfAnotherUserIsReject() {
            ShardedId first = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, first);

            ShardedId second = newUserId();

            assertThatThrownBy(()-> gateway.cancelReserve(EMAIL_IDENTITY_IDENTIFIER, second)).isInstanceOf(IdentifierNotOwnedByUserException.class);
            ;
        }
    }

    @Nested
    @DisplayName("4. 开始释放")
    @Order(Order.DEFAULT + 4)
    class BeginRelease{

        @Test
        @DisplayName("释放中的Lookup 不能路由到 对应的 user")
        void releasingIdentityNotVisibleToLoginRouting(){
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);

            gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, userId);
            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).isEmpty();
        }

        @Test
        @DisplayName("beginRelease 申请释放Lookup，是幂等的")
        void beginReleaseIsIdempotent(){
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);

            gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, userId);
            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).isEmpty();
        }

        @Test
        @DisplayName("beginRelease 释放一个未确认的记录是被拒绝的（合法状态的前一个状态）")
        void beginReleaseAReservationIsReject(){
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);

            assertThatThrownBy(()->gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, userId))
                    .isInstanceOf(InvalidIdentifierStatusException.class);
        }

        @Test
        @DisplayName("beginRelease 释放一个不存在的记录是被拒绝的（当前操作的下一个状态）")
        void beginReleaseAReservationNotExistIsReject(){
            ShardedId userId = newUserId();
            assertThatThrownBy(()->gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, userId))
                    .isInstanceOf(IdentifierNotExistException.class);
        }
        @Test
        @DisplayName("beginRelease 释放一个不存在的记录是被拒绝的（当前操作的下一个状态）")
        void beginReleaseAReservationOfAnotherUserIsReject(){
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);
            assertThatThrownBy(()->gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, newUserId()))
                    .isInstanceOf(IdentifierNotOwnedByUserException.class);
        }

    }


    @Nested
    @DisplayName("5. 完成释放")
    @Order(Order.DEFAULT + 5)
    class CompleteRelease{

        @Test
        @DisplayName("completeRelease 释放完成的Lookup 不能路由到 对应的 use， 且已被删除")
        void releasedIdentityNotVisibleToLoginRouting(){
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.completeRelease(EMAIL_IDENTITY_IDENTIFIER, userId);
            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).isEmpty();
            assertThat(selectByIdentifier(EMAIL_IDENTITY_IDENTIFIER)).isNull();
        }

        @Test
        @DisplayName("completeRelease 完成释放是幂等的")
        void completeReleaseIsIdempotent(){
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.completeRelease(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.completeRelease(EMAIL_IDENTITY_IDENTIFIER, userId);
            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).isEmpty();
            assertThat(selectByIdentifier(EMAIL_IDENTITY_IDENTIFIER)).isNull();
        }
        @Test
        @DisplayName("completeRelease 一个未确认的展示被拒绝的")
        void completeReleaseReservingIsReject(){
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            assertThatThrownBy(()->gateway.completeRelease(EMAIL_IDENTITY_IDENTIFIER, userId))
                    .isInstanceOf(InvalidIdentifierStatusException.class);
        }

        @Test
        @DisplayName("completeRelease 其它人的记录是拒绝的")
        void completeReleaseOfAnotherUserIsReject(){
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, userId);
            assertThatThrownBy(()->gateway.completeRelease(EMAIL_IDENTITY_IDENTIFIER, newUserId()))
                    .isInstanceOf(IdentifierNotOwnedByUserException.class);
        }

    }

    @Nested
    @DisplayName("6. 回滚释放")
    @Order(Order.DEFAULT + 6)
    class AbortRelease{
        @Test
        @DisplayName("abortRelease取消的释放中记录，可以查到和正常路由到用户")
        void abortedReleasingIdentityNotVisibleToLoginRouting(){
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.abortRelease(EMAIL_IDENTITY_IDENTIFIER, userId);
            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).isNotEmpty();
            assertThat(selectByIdentifier(EMAIL_IDENTITY_IDENTIFIER)).isNotNull();
        }

        @Test
        @DisplayName("abortRelease是幂等的")
        void abortReleasingIdentityIsIdempotent(){
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.abortRelease(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.abortRelease(EMAIL_IDENTITY_IDENTIFIER, userId);
            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).isNotEmpty();
            assertThat(selectByIdentifier(EMAIL_IDENTITY_IDENTIFIER)).isNotNull();
        }
        @Test
        @DisplayName("abortRelease 未确认的记录，是被拒绝的")
        void abortConfirmedIsReject(){
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            assertThatThrownBy(()->gateway.abortRelease(EMAIL_IDENTITY_IDENTIFIER, userId))
                    .isInstanceOf(InvalidIdentifierStatusException.class);
        }
        @Test
        @DisplayName("abortRelease 不存在记录，是被拒绝的")
        void abortIdentifierNotExistIsReject(){
            ShardedId userId = newUserId();
            assertThatThrownBy(()->gateway.abortRelease(EMAIL_IDENTITY_IDENTIFIER, userId))
                    .isInstanceOf(IdentifierNotExistException.class);
        }
        @Test
        @DisplayName("abortRelease 其它人的记录，是被拒绝的")
        void abortReleasingIdentityConfirmedIsReject(){
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.abortRelease(EMAIL_IDENTITY_IDENTIFIER, userId);
            assertThatThrownBy(()->gateway.abortRelease(EMAIL_IDENTITY_IDENTIFIER, newUserId()))
                    .isInstanceOf(IdentifierNotOwnedByUserException.class);
        }
    }


    @Nested
    @DisplayName("7. 不同的用户")
    @Order(Order.DEFAULT + 7)
    class AnotherUser{

        @Test
        @DisplayName("release 后，标识可以被新用户重新 reserve 并 confirm")
        void identifierCanBeReservedAgainAfterRelease() {
            ShardedId first = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, first);
            gateway.cancelReserve(EMAIL_IDENTITY_IDENTIFIER, first);

            ShardedId second = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, second);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, second);

            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).contains(second);
        }


    }


    @Nested
    @DisplayName("8. 回收占位")
    class ReclaimReserved {

        @Test
        @DisplayName("过期的 RESERVED 记录在身份行不存在时被 reclaimReservedStale 删除")
        void reclaimDeletesStalePendingWhenIdentityRowMissing() {
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, newUserId());
            backdate(EMAIL_IDENTITY_IDENTIFIER);

            int handled = gateway.reclaimReservedStale(Duration.ofMinutes(30), 100, pending -> false);

            assertThat(handled).isEqualTo(1);
            assertThat(count()).isZero();
        }

        @Test
        @DisplayName("过期的 RESERVED 记录在身份行已存在时被 reclaimReservedStale 提升为 ACTIVE")
        void reclaimPromotesStalePendingWhenIdentityRowExists() {
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            backdate(EMAIL_IDENTITY_IDENTIFIER);

            gateway.reclaimReservedStale(Duration.ofMinutes(30), 100, pending -> true);

            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).contains(userId);
        }

        @Test
        @DisplayName("reclaimReservedStale 忽略未过期的 RESERVED 和所有 ACTIVE 记录")
        void reclaimIgnoresRecentPendingAndConfirmed() {
            IdentityIdentifier anotherEmail = EmailIdentifier.from("b@example.com");

            ShardedId confirmedUser = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, confirmedUser);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, confirmedUser);
            backdate(EMAIL_IDENTITY_IDENTIFIER);
            gateway.reserve(anotherEmail, newUserId()); // 未过期

            int handled = gateway.reclaimReservedStale(Duration.ofMinutes(30), 100, pending -> false);

            assertThat(handled).isZero();
            assertThat(count()).isEqualTo(2);
        }



    }

    @Nested
    @DisplayName("9. 回收释放中的")
    class ReclaimReleasing {

        @Test
        @DisplayName("过期的 RELEASING 记录在身份行不存在时被 reclaimReleasingStale 删除")
        void reclaimDeletesStalePendingWhenIdentityRowMissing() {
            newReleasingUser();
            backdate(EMAIL_IDENTITY_IDENTIFIER);

            int handled = gateway.reclaimReleasingStale(Duration.ofMinutes(30), 100, pending -> false);
            assertThat(handled).isEqualTo(1);
            assertThat(count()).isZero();
        }

        @Test
        @DisplayName("过期的 RELEASING 记录在身份行已存在时被 reclaimReleasingStale 还原为 ACTIVE")
        void reclaimPromotesStalePendingWhenIdentityRowExists() {
            ShardedId userId = newReleasingUser();
            backdate(EMAIL_IDENTITY_IDENTIFIER);

            gateway.reclaimReleasingStale(Duration.ofMinutes(30), 100, pending -> true);

            assertThat(gateway.findUserId(EMAIL_IDENTITY_IDENTIFIER)).contains(userId);
        }

        @Test
        @DisplayName("reclaimReleasingStale 忽略未过期的 RELEASING 和所有 ACTIVE 记录")
        void reclaimIgnoresRecentPendingAndConfirmed() {
            IdentityIdentifier anotherEmail = EmailIdentifier.from("b@example.com");

            ShardedId confirmedUserId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, confirmedUserId);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, confirmedUserId);
            backdate(EMAIL_IDENTITY_IDENTIFIER);
            ShardedId unStaledUserId = newUserId();
            gateway.reserve(anotherEmail, unStaledUserId);
            gateway.confirm(anotherEmail, unStaledUserId);
            gateway.beginRelease(anotherEmail, unStaledUserId);// 未过期

            int handled = gateway.reclaimReleasingStale(Duration.ofMinutes(30), 100, pending -> false);

            assertThat(handled).isZero();
            assertThat(count()).isEqualTo(2);
        }
        private ShardedId newReleasingUser(){
            ShardedId userId = newUserId();
            gateway.reserve(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.confirm(EMAIL_IDENTITY_IDENTIFIER, userId);
            gateway.beginRelease(EMAIL_IDENTITY_IDENTIFIER, userId);
            return userId;
        }
    }


    // ------------------------------------------------------------------

    private void backdate(IdentityIdentifier identifier) {
        jdbc.update(
                "UPDATE identity_lookup SET updated_at = DATE_SUB(NOW(3), INTERVAL 1 HOUR) "
                        + "WHERE identity_type = ? AND identifier = ?",
                identifier.type().getCode(), identifier.value());
    }

    private int count() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM identity_lookup", Integer.class);
        return n == null ? 0 : n;
    }


    private ShardedId newUserId() {
        return idGenerator.nextId(IdGenerationCommand.builder("auth_test_user").build());
    }
}
