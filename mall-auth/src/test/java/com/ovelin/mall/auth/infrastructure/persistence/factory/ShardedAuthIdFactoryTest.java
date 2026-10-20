package com.ovelin.mall.auth.infrastructure.persistence.factory;

import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifier;
import com.ovelin.mall.auth.domain.model.identityIdentifier.PhoneIdentifier;
import com.ovelin.mall.common.sharding.core.api.ShardResolver;
import com.ovelin.mall.common.sharding.core.ov.ShardId;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.id.generator.api.BusinessType;
import com.ovelin.mall.id.generator.api.IdGenerationCommand;
import com.ovelin.mall.id.generator.api.IdGenerator;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("根据身份标识解析分片并生成用户 ID")
    void newUserIdByIdentifierShard() {
        ShardId shard = ShardId.of(1);
        IdentityIdentifier phoneIdentifier = PhoneIdentifier.parseRawInput("13800000000");
        ShardedId expected = mock(ShardedId.class);
        when(shardResolver.resolve(phoneIdentifier.type().getCode(), phoneIdentifier.value())).thenReturn(shard);
        when(idGenerator.nextId(any())).thenReturn(expected);

        ShardedId actual = factory.newUserId(phoneIdentifier);

        assertThat(actual).isSameAs(expected);
        ArgumentCaptor<IdGenerationCommand> captor = ArgumentCaptor.forClass(IdGenerationCommand.class);
        verify(idGenerator).nextId(captor.capture());
        assertThat(captor.getValue().businessType()).isEqualTo(new BusinessType("user"));
        assertThat(captor.getValue().shardId()).contains(shard);

        verify(shardResolver).resolve(
                phoneIdentifier.type().getCode(),
                phoneIdentifier.value()
        );
    }

    @Test
    @DisplayName("生成身份 ID 时沿用用户所在分片")
    void newIdentityIdByUserShard() {
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

    @Test
    @DisplayName("生成 MFA 因子 ID 时沿用用户所在分片")
    void newFactorIdByUserShard() {
        ShardId shard = ShardId.of(0);
        ShardedId userId = mock(ShardedId.class);
        when(userId.shardId()).thenReturn(shard);
        when(idGenerator.nextId(any())).thenReturn(mock(ShardedId.class));

        factory.newFactorId(userId);

        ArgumentCaptor<IdGenerationCommand> captor = ArgumentCaptor.forClass(IdGenerationCommand.class);
        verify(idGenerator).nextId(captor.capture());
        assertThat(captor.getValue().businessType()).isEqualTo(new BusinessType("user_mfa_factor"));
        assertThat(captor.getValue().shardId()).contains(shard);
    }
}