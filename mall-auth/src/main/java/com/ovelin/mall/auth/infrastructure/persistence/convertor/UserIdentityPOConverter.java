package com.ovelin.mall.auth.infrastructure.persistence.convertor;

import com.ovelin.mall.auth.domain.model.aggregate.UserIdentity;
import com.ovelin.mall.auth.infrastructure.persistence.po.IdentityLookupPO;
import com.ovelin.mall.auth.infrastructure.persistence.po.UserIdentityPO;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;

public final class UserIdentityPOConverter {

    private UserIdentityPOConverter() {
    }

    public static UserIdentity toDomain(UserIdentityPO po) {
        return UserIdentity.reconstitute(
                ShardedId.fromId(po.getId()), ShardedId.fromId(po.getUserId()),
                po.getIdentityType(), po.getIdentifier(),
               po.getVerifiedAt(), po.getLastLoginAt());
    }

    public static UserIdentityPO toPO(UserIdentity identity) {
        UserIdentityPO po = new UserIdentityPO();
        po.setId(identity.getId().value());
        po.setUserId(identity.getUserId().value());
        po.setIdentityType(identity.type().getCode());
        po.setIdentifier(identity.value());
        po.setVerifiedAt(identity.getVerifiedAt());
        po.setLastLoginAt(identity.getLastLoginAt());
        return po;
    }

    public static IdentityLookupPO toLookupPO(UserIdentity identity) {
        IdentityLookupPO po = new IdentityLookupPO();
        po.setIdentityType(identity.type().getCode());
        po.setIdentifier(identity.value());
        po.setUserId(identity.getUserId().value());
        return po;
    }
}