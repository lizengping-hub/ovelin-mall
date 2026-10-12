package com.ovelin.mall.auth.infrastructure.persistence.convertor;

import com.ovelin.mall.auth.domain.model.aggregate.MfaFactor;
import com.ovelin.mall.auth.domain.model.enums.MfaFactorStatus;
import com.ovelin.mall.auth.domain.model.enums.MfaFactorType;
import com.ovelin.mall.auth.infrastructure.persistence.po.UserMfaFactorPO;

public final class MfaFactorPOConverter {

    private MfaFactorPOConverter() {
    }

    public static MfaFactor toDomain(UserMfaFactorPO po) {
        return MfaFactor.restore(
                po.getId(), po.getUserId(), MfaFactorType.fromCode(po.getFactorType()),
                po.getIdentityId(), po.getSecretEncrypted(), MfaFactorStatus.fromCode(po.getStatus()),
                po.getVerifiedAt(), po.getLastUsedAt());
    }

    public static UserMfaFactorPO toPO(MfaFactor factor) {
        UserMfaFactorPO po = new UserMfaFactorPO();
        po.setId(factor.getId());
        po.setUserId(factor.getUserId());
        po.setFactorType(factor.getType().getCode());
        po.setIdentityId(factor.getIdentityId());
        po.setSecretEncrypted(factor.getSecretEncrypted());
        po.setStatus(factor.getStatus().getCode());
        po.setVerifiedAt(factor.getVerifiedAt());
        po.setLastUsedAt(factor.getLastUsedAt());
        return po;
    }
}