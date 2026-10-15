package com.ovelin.mall.auth.infrastructure.persistence.convertor;

import com.ovelin.mall.auth.domain.model.aggregate.MfaFactor;
import com.ovelin.mall.auth.domain.model.enums.MfaFactorStatus;
import com.ovelin.mall.auth.domain.model.enums.MfaFactorType;
import com.ovelin.mall.auth.infrastructure.persistence.po.UserMfaFactorPO;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;

public final class MfaFactorPOConverter {

    private MfaFactorPOConverter() {
    }

    public static MfaFactor toDomain(UserMfaFactorPO po) {
        return MfaFactor.reconstitute(
                ShardedId.fromId(po.getId()), ShardedId.fromId(po.getUserId()),
                MfaFactorType.fromCode(po.getFactorType()),
                po.getIdentityId() == null ? null : ShardedId.fromId(po.getIdentityId()),
                po.getSecretEncrypted(), MfaFactorStatus.fromCode(po.getStatus()),
                po.getVerifiedAt(), po.getLastUsedAt());
    }

    public static UserMfaFactorPO toPO(MfaFactor factor) {
        UserMfaFactorPO po = new UserMfaFactorPO();
        po.setId(factor.getId().value());
        po.setUserId(factor.getUserId().value());
        po.setFactorType(factor.getType().getCode());
        po.setIdentityId(factor.getIdentityId() == null ? null : factor.getIdentityId().value());
        po.setSecretEncrypted(factor.getSecretEncrypted());
        po.setStatus(factor.getStatus().getCode());
        po.setVerifiedAt(factor.getVerifiedAt());
        po.setLastUsedAt(factor.getLastUsedAt());
        return po;
    }
}