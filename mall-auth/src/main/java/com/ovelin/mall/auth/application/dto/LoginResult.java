package com.ovelin.mall.auth.application.dto;

import com.ovelin.mall.auth.domain.model.enums.MfaFactorType;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;

import java.util.List;

public record LoginResult(ShardedId userId, boolean mfaRequired, List<MfaFactorType> availableFactors) {
}