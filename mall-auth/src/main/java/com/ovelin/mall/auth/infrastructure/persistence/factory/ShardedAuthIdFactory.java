package com.ovelin.mall.auth.infrastructure.persistence.factory;

import com.ovelin.mall.auth.application.port.AuthIdFactory;
import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifier;
import com.ovelin.mall.common.sharding.core.api.ShardResolver;
import com.ovelin.mall.common.sharding.core.ov.ShardId;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.id.generator.api.BusinessType;
import com.ovelin.mall.id.generator.api.IdGenerationCommand;
import com.ovelin.mall.id.generator.api.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class ShardedAuthIdFactory implements AuthIdFactory {

    private static final BusinessType USER = new BusinessType("user");
    private static final BusinessType USER_IDENTITY = new BusinessType("user_identity");
    private static final BusinessType USER_MFA_FACTOR = new BusinessType("user_mfa_factor");

    private final ShardResolver shardResolver;
    private final IdGenerator idGenerator;

    @Override
    public ShardedId newUserId(IdentityIdentifier identifier) {
        ShardId shardId = shardResolver.resolve(identifier.type().getCode(), identifier.value());
        return next(USER, shardId);
    }

    @Override
    public ShardedId newIdentityId(ShardedId userId) {
        return next(USER_IDENTITY, userId.shardId());
    }

    @Override
    public ShardedId newFactorId(ShardedId userId) {
        return next(USER_MFA_FACTOR, userId.shardId());
    }

    private ShardedId next(BusinessType businessType, ShardId shardId) {
        return idGenerator.nextId(IdGenerationCommand.builder(businessType)
                .shardId(shardId)
                .build());
    }
}