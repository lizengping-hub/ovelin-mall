package com.ovelin.mall.auth.domain.model.snapshot;

import com.ovelin.mall.auth.domain.model.enums.MfaFactorStatus;
import com.ovelin.mall.auth.domain.model.enums.MfaFactorType;

import java.time.Instant;

public record MfaFactorSnapshot(long id, long userId, MfaFactorType type, Long identityId,
                                String secretEncrypted, MfaFactorStatus status,
                                Instant verifiedAt, Instant lastUsedAt) {
}