package com.ovelin.mall.id.generator.starter.domain.module.service;

import com.ovelin.mall.common.sharding.core.api.ShardSelector;
import com.ovelin.mall.common.sharding.core.ov.ShardId;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.Allocations;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.SequenceName;
import com.ovelin.mall.id.generator.starter.domain.service.IdGeneratorDomainService;
import com.ovelin.mall.id.generator.starter.domain.service.SequenceDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * mock 掉序列服务和 SharSelector:分片为 null 走 selector、指定分片不碰 selector、count<=0 提前失败、异常透传
 */
@ExtendWith(MockitoExtension.class)
class IdGeneratorDomainServiceTest {

    // TODO: 按 SequenceName 的实际构造方式调整
    static final SequenceName ORDER = new SequenceName("order");
    static final ShardId RESOLVED = ShardId.of(3);
    static final ShardId SPECIFIED = ShardId.of(7);

    @Mock
    SequenceDomainService sequenceService;
    @Mock
    ShardSelector shardSelector;

    IdGeneratorDomainService domainService;

    @BeforeEach
    void setUp() {
        domainService = new IdGeneratorDomainService(sequenceService, shardSelector);
    }

    // ---------- nextId ----------

    @Test
    void nextIdShouldUseResolverWhenShardNotSpecified() {
        when(shardSelector.select()).thenReturn(RESOLVED);
        when(sequenceService.nextValue(ORDER, RESOLVED)).thenReturn(12345L);

        ShardedId id = domainService.nextId(ORDER, null);

        assertEquals(RESOLVED, id.shardId());
        assertEquals(12345L, id.sequence());
    }

    @Test
    void nextIdShouldUseSpecifiedShardAndSkipResolver() {
        when(sequenceService.nextValue(ORDER, SPECIFIED)).thenReturn(1L);

        ShardedId id = domainService.nextId(ORDER, SPECIFIED);

        assertEquals(SPECIFIED, id.shardId());
        verifyNoInteractions(shardSelector);
    }

    @Test
    void nextIdShouldRoundTripThroughFromId() {
        when(sequenceService.nextValue(ORDER, SPECIFIED)).thenReturn(999L);

        ShardedId id = domainService.nextId(ORDER, SPECIFIED);
        ShardedId parsed = ShardedId.fromId(id.value());

        assertEquals(SPECIFIED, parsed.shardId());
        assertEquals(999L, parsed.sequence());
    }

    @Test
    void nextIdShouldPropagateSequenceExhaustion() {
        when(sequenceService.nextValue(ORDER, SPECIFIED))
                .thenThrow(new IllegalStateException("exhausted"));

        assertThrows(IllegalStateException.class, () -> domainService.nextId(ORDER, SPECIFIED));
    }

    // ---------- nextIds ----------

    @Test
    void nextIdsShouldUseResolverAndMapEveryValue() {
        // 假设 Allocations.toList() 返回 List<Long>;若 Allocations 是 final 类,需要 mockito 5+(inline mock maker)
        Allocations allocations = mock(Allocations.class);
        when(allocations.toList()).thenReturn(List.of(10L, 11L, 12L));
        when(shardSelector.select()).thenReturn(RESOLVED);
        when(sequenceService.nextValues(ORDER, RESOLVED, 3)).thenReturn(allocations);

        List<ShardedId> ids = domainService.nextIds(ORDER, null, 3);

        assertEquals(List.of(10L, 11L, 12L), ids.stream().map(ShardedId::sequence).toList());
        assertTrue(ids.stream().allMatch(id -> id.shardId().equals(RESOLVED)),
                "all ids of one batch must be in the same shard");
    }

    @Test
    void nextIdsShouldUseSpecifiedShardAndSkipResolver() {
        Allocations allocations = mock(Allocations.class);
        when(allocations.toList()).thenReturn(List.of(1L, 2L));
        when(sequenceService.nextValues(ORDER, SPECIFIED, 2)).thenReturn(allocations);

        List<ShardedId> ids = domainService.nextIds(ORDER, SPECIFIED, 2);

        assertEquals(2, ids.size());
        verifyNoInteractions(shardSelector);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void nextIdsShouldRejectNonPositiveCountBeforeTouchingCollaborators(int count) {
        assertThrows(IllegalArgumentException.class,
                () -> domainService.nextIds(ORDER, SPECIFIED, count));

        verifyNoInteractions(sequenceService, shardSelector);
    }
}
