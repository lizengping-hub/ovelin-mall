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
import org.springframework.dao.DuplicateKeyException;

import java.util.ConcurrentModificationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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

    @Test
    @DisplayName("reserve：插入冲突但查不到冲突记录（冲突方刚被释放），重试一次后仍冲突，抛出 IdentifierAlreadyTakenException")
    void reserveThrowsAlreadyTakenWhenConflictDisappearsButRetryStillConflicts() {
        // insert 始终抛主键冲突；selectByIdentifier 默认返回 null（记录已不可见）
        doThrow(new DuplicateKeyException("duplicate")).when(mapper).insert(any(IdentityLookupPO.class));

        assertThatThrownBy(() -> gateway().reserve(IDENTIFIER, userId(1L)))
                .isInstanceOf(IdentifierAlreadyTakenException.class);

        verify(mapper, times(2)).insert(any(IdentityLookupPO.class));   // 首次 + 重试一次，不会无限重试
    }

    @Test
    @DisplayName("cancelReserve：属主和状态都匹配却没删掉（读写之间被并发修改），抛出 ConcurrentModificationException")
    void cancelReserveThrowsWhenRowChangedBetweenDeleteAndRead() {
        ShardedId owner = userId(1L);
        // deleteIfMatch 默认返回 0（没删掉）；随后读到的记录却仍属于自己且仍是 RESERVED
        when(mapper.selectByIdentifier(IDENTIFIER.type().getCode(), IDENTIFIER.value()))
                .thenReturn(existingRow(owner, IdentityLookupStatus.RESERVED));

        assertThatThrownBy(() -> gateway().cancelReserve(IDENTIFIER, owner))
                .isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    @DisplayName("completeRelease：属主和状态都匹配却没删掉（读写之间被并发修改），抛出 ConcurrentModificationException")
    void completeReleaseThrowsWhenRowChangedBetweenDeleteAndRead() {
        ShardedId owner = userId(1L);
        when(mapper.selectByIdentifier(IDENTIFIER.type().getCode(), IDENTIFIER.value()))
                .thenReturn(existingRow(owner, IdentityLookupStatus.RELEASING));

        assertThatThrownBy(() -> gateway().completeRelease(IDENTIFIER, owner))
                .isInstanceOf(ConcurrentModificationException.class);
    }
}