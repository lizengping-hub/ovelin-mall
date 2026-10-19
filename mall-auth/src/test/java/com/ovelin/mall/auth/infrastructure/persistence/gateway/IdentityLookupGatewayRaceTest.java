package com.ovelin.mall.auth.infrastructure.persistence.gateway;

import com.ovelin.mall.auth.domain.exception.IdentifierAlreadyTakenException;
import com.ovelin.mall.auth.domain.model.enums.IdentityLookupStatus;
import com.ovelin.mall.auth.domain.model.identityIdentifier.EmailIdentifier;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifier;
import com.ovelin.mall.auth.infrastructure.persistence.mapper.IdentityLookupMapper;
import com.ovelin.mall.auth.infrastructure.persistence.po.IdentityLookupPO;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ConcurrentModificationException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用 mock 的 mapper 构造集成测试里很难稳定复现的并发竞态分支。
 * 不启动 Spring，所以 @Transactional 不生效，这里只关心 gateway 自身的分支逻辑。
 */
@ExtendWith(MockitoExtension.class)
class IdentityLookupGatewayRaceTest {

    private static final IdentityIdentifier IDENTIFIER = EmailIdentifier.from("a@example.com");

    @Mock
    private IdentityLookupMapper mapper;

    private IdentityLookupGateway gateway() {
        return new IdentityLookupGateway(mapper);
    }

    /** 注意：按 ShardedId.fromId 的实际入参类型调整（这里假设为 long） */
    private static ShardedId userId(long id) {
        return ShardedId.fromId(id);
    }

    private static IdentityLookupPO existingRow(ShardedId owner, IdentityLookupStatus status) {
        IdentityLookupPO po = new IdentityLookupPO();
        po.setIdentityType(IDENTIFIER.type().getCode());
        po.setIdentifier(IDENTIFIER.value());
        po.setUserId(owner.value());
        po.setStatus(status.getCode());
        return po;
    }

    private void givenRowExists(ShardedId owner, IdentityLookupStatus status) {
        when(mapper.selectByIdentifier(IDENTIFIER.type().getCode(), IDENTIFIER.value()))
                .thenReturn(existingRow(owner, status));
    }

    // ------------------------------------------------------------------ reserve

    @Test
    @DisplayName("reserve：upsert 之后查不到记录（冲突方恰好被释放），抛出 IdentifierAlreadyTakenException")
    void reserveThrowsAlreadyTakenWhenRowDisappearsAfterUpsert() {
        // insertOrTouch 默认返回 0；selectByIdentifier 默认返回 null（记录已不可见）
        assertThatThrownBy(() -> gateway().reserve(IDENTIFIER, userId(1L)))
                .isInstanceOf(IdentifierAlreadyTakenException.class);

        verify(mapper, times(1)).insertOrTouch(any(), any(), any(), anyInt());
    }

    // ------------------------------------------------------------------ cancelReserve / completeRelease

    @Test
    @DisplayName("cancelReserve：属主和状态都匹配却连续两次没删掉（读写之间被并发修改），抛出 ConcurrentModificationException")
    void cancelReserveThrowsWhenDeleteKeepsMissingAMatchingRow() {
        ShardedId owner = userId(1L);
        givenRowExists(owner, IdentityLookupStatus.RESERVED);   // deleteIfMatch 默认返回 0

        assertThatThrownBy(() -> gateway().cancelReserve(IDENTIFIER, owner))
                .isInstanceOf(ConcurrentModificationException.class);

        verify(mapper, times(2)).deleteIfMatch(any(), any(), anyLong(), any());   // 首次 + 重试一次
    }

    @Test
    @DisplayName("completeRelease：属主和状态都匹配却连续两次没删掉（读写之间被并发修改），抛出 ConcurrentModificationException")
    void completeReleaseThrowsWhenDeleteKeepsMissingAMatchingRow() {
        ShardedId owner = userId(1L);
        givenRowExists(owner, IdentityLookupStatus.RELEASING);

        assertThatThrownBy(() -> gateway().completeRelease(IDENTIFIER, owner))
                .isInstanceOf(ConcurrentModificationException.class);

        verify(mapper, times(2)).deleteIfMatch(any(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("cancelReserve：第一次没删掉（读写之间被并发修改），重试一次删除成功，不抛异常")
    void cancelReserveSucceedsWhenRetryDeleteSucceeds() {
        ShardedId owner = userId(1L);
        givenRowExists(owner, IdentityLookupStatus.RESERVED);
        when(mapper.deleteIfMatch(IDENTIFIER.type().getCode(), IDENTIFIER.value(), owner.value(), IdentityLookupStatus.RESERVED))
                .thenReturn(0, 1);

        assertThatCode(() -> gateway().cancelReserve(IDENTIFIER, owner)).doesNotThrowAnyException();

        verify(mapper, times(2)).deleteIfMatch(any(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("completeRelease：第一次没删掉（读写之间被并发修改），重试一次删除成功，不抛异常")
    void completeReleaseSucceedsWhenRetryDeleteSucceeds() {
        ShardedId owner = userId(1L);
        givenRowExists(owner, IdentityLookupStatus.RELEASING);
        when(mapper.deleteIfMatch(IDENTIFIER.type().getCode(), IDENTIFIER.value(), owner.value(), IdentityLookupStatus.RELEASING))
                .thenReturn(0, 1);

        assertThatCode(() -> gateway().completeRelease(IDENTIFIER, owner)).doesNotThrowAnyException();

        verify(mapper, times(2)).deleteIfMatch(any(), any(), anyLong(), any());
    }
}