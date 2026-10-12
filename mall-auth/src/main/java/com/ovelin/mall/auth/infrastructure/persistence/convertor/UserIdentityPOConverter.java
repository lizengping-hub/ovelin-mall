package com.ovelin.mall.auth.infrastructure.persistence.convertor;

import com.ovelin.mall.auth.domain.model.aggregate.UserIdentity;
import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import com.ovelin.mall.auth.infrastructure.persistence.po.IdentityLookupPO;
import com.ovelin.mall.auth.infrastructure.persistence.po.UserIdentityPO;

public final class UserIdentityPOConverter {

    private UserIdentityPOConverter() {
    }

    public static UserIdentity toDomain(UserIdentityPO po) {
        return UserIdentity.restore(
                po.getId(), po.getUserId(), IdentityType.fromCode(po.getIdentityType()),
                po.getIdentifier(), Boolean.TRUE.equals(po.getPrimary()),
                po.getVerifiedAt(), po.getLastLoginAt());
    }

    public static UserIdentityPO toPO(UserIdentity identity) {
        UserIdentityPO po = new UserIdentityPO();
        po.setId(identity.getId());
        po.setUserId(identity.getUserId());
        po.setIdentityType(identity.getType().getCode());
        po.setIdentifier(identity.getIdentifier());
        po.setPrimary(identity.isPrimary());
        po.setVerifiedAt(identity.getVerifiedAt());
        po.setLastLoginAt(identity.getLastLoginAt());
        return po;
    }

    public static IdentityLookupPO toLookupPO(UserIdentity identity) {
        IdentityLookupPO po = new IdentityLookupPO();
        po.setIdentityType(identity.getType().getCode());
        po.setIdentifier(identity.getIdentifier());
        po.setUserId(identity.getUserId());
        return po;
    }
}