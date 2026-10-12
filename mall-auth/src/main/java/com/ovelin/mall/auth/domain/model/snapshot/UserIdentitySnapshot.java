package com.ovelin.mall.auth.domain.model.snapshot;

import com.ovelin.mall.auth.domain.model.enums.IdentityType;

import java.time.Instant;

public record UserIdentitySnapshot(long id, long userId, IdentityType type, String identifier,
                                   boolean primary, Instant verifiedAt, Instant lastLoginAt) {
}