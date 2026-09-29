package com.ovelin.mall.id.generator.starter;

import com.ovelin.mall.common.sharding.core.ov.ShardId;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.id.generator.api.IdDTO;
import com.ovelin.mall.id.generator.api.IdGenerationCommand;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.SequenceName;
import com.ovelin.mall.id.generator.starter.domain.service.IdGeneratorDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * mock 掉 IdGeneratorDomainService:Optional<ShardId> 转 null、count 透传、DTO 映射
 */
@ExtendWith(MockitoExtension.class)
class IdGeneratorAppServiceTest {

    @Mock IdGeneratorDomainService domainService;

    IdGeneratorAppService appService;

    @BeforeEach
    void setUp() {
        appService = new IdGeneratorAppService(domainService);
    }

    @Test
    void nextIdShouldPassNullShardWhenNotSpecifiedAndMapToDto() {
        ShardedId sharded = ShardedId.of(3, 42L);
        when(domainService.nextId(any(SequenceName.class), isNull())).thenReturn(sharded);

        IdDTO dto = appService.nextId(IdGenerationCommand.builder("order").build());

        assertEquals(sharded.value(), dto.id()); // TODO: 若 IdAssembler 还映射了其他字段,在这里补充断言
        verify(domainService).nextId(any(SequenceName.class), isNull());
        verifyNoMoreInteractions(domainService);
    }

    @Test
    void nextIdShouldPassSpecifiedShard() {
        when(domainService.nextId(any(SequenceName.class), eq(ShardId.of(5))))
                .thenReturn(ShardedId.of(5, 1L));

        IdDTO dto = appService.nextId(IdGenerationCommand.builder("order").shardId(5).build());

        assertEquals(5, ShardedId.shardOf(dto.id()));
    }

    @Test
    void nextIdShouldConvertBusinessTypeToSequenceName() {
        when(domainService.nextId(any(SequenceName.class), isNull()))
                .thenReturn(ShardedId.of(0, 1L));

        appService.nextId(IdGenerationCommand.builder("order").build());

        ArgumentCaptor<SequenceName> captor = ArgumentCaptor.forClass(SequenceName.class);
        verify(domainService).nextId(captor.capture(), isNull());
        // TODO: 按 IdAssembler.toSequenceName 的真实映射调整(若是原样映射则如下)
        assertEquals("order", captor.getValue().value());
    }

    @Test
    void nextIdsShouldPassCountAndShardAndMapAll() {
        when(domainService.nextIds(any(SequenceName.class), eq(ShardId.of(2)), eq(3)))
                .thenReturn(List.of(ShardedId.of(2, 1L), ShardedId.of(2, 2L), ShardedId.of(2, 3L)));

        List<IdDTO> dtos = appService.nextIds(
                IdGenerationCommand.builder("order").shardId(2).count(3).build());

        assertEquals(3, dtos.size());
        assertEquals(List.of(1L, 2L, 3L),
                dtos.stream().map(d -> ShardedId.fromId(d.id()).sequence()).toList());
    }

    @Test
    void nextIdsShouldPassNullShardWhenNotSpecified() {
        when(domainService.nextIds(any(SequenceName.class), isNull(), eq(2)))
                .thenReturn(List.of(ShardedId.of(1, 1L), ShardedId.of(1, 2L)));

        List<IdDTO> dtos = appService.nextIds(IdGenerationCommand.builder("order").count(2).build());

        assertEquals(2, dtos.size());
    }

    @Test
    void shouldPropagateDomainException() {
        when(domainService.nextId(any(SequenceName.class), isNull()))
                .thenThrow(new IllegalStateException("exhausted"));

        assertThrows(IllegalStateException.class,
                () -> appService.nextId(IdGenerationCommand.builder("order").build()));
    }
}
