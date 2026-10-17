package com.ovelin.mall.auth.infrastructure.persistence.factory;

import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import com.ovelin.mall.common.sharding.core.api.ShardResolver;
import com.ovelin.mall.common.sharding.core.ov.ShardId;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.id.generator.api.BusinessType;
import com.ovelin.mall.id.generator.api.IdGenerationCommand;
import com.ovelin.mall.id.generator.api.IdGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShardedAuthIdFactoryTest {

    @Mock
    ShardResolver shardResolver;
    @Mock
    IdGenerator idGenerator;
    @InjectMocks
    ShardedAuthIdFactory factory;

    @Test
    void newUserId_使用标识哈希出的分片() {
        ShardId shard = ShardId.of(1);
        ShardedId expected = mock(ShardedId.class);
        when(shardResolver.resolve("phone", "13800000000")).thenReturn(shard);
        when(idGenerator.nextId(any())).thenReturn(expected);

        ShardedId actual = factory.newUserId(IdentityType.PHONE, "13800000000");

        assertThat(actual).isSameAs(expected);
        ArgumentCaptor<IdGenerationCommand> captor = ArgumentCaptor.forClass(IdGenerationCommand.class);
        verify(idGenerator).nextId(captor.capture());
        assertThat(captor.getValue().businessType()).isEqualTo(new BusinessType("user"));
        assertThat(captor.getValue().shardId()).contains(shard);
    }

    @Test
    void newIdentityId_沿用用户所在分片() {
        ShardId shard = ShardId.of(0);
        ShardedId userId = mock(ShardedId.class);
        when(userId.shardId()).thenReturn(shard);
        when(idGenerator.nextId(any())).thenReturn(mock(ShardedId.class));

        factory.newIdentityId(userId);

        ArgumentCaptor<IdGenerationCommand> captor = ArgumentCaptor.forClass(IdGenerationCommand.class);
        verify(idGenerator).nextId(captor.capture());
        assertThat(captor.getValue().businessType()).isEqualTo(new BusinessType("user_identity"));
        assertThat(captor.getValue().shardId()).contains(shard);
    }
}