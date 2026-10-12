package com.ovelin.mall.auth.domain.model.snapshot;

import java.time.Instant;

public record UserPasswordSnapshot(long userId, String passwordHash, Instant passwordChangedAt,
                                   int failedAttempts, Instant lockedUntil) {
}