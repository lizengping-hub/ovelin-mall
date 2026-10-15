package com.ovelin.mall.auth.domain.model.aggregate;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;

@Getter
public class UserPassword {

    private final ShardedId userId;
    private String passwordHash;
    private Instant passwordChangedAt;
    private int failedAttempts;
    private Instant lockedUntil;

    private UserPassword(ShardedId userId, String passwordHash, Instant passwordChangedAt,
                         int failedAttempts, Instant lockedUntil) {
        this.userId = userId;
        this.passwordHash = passwordHash;
        this.passwordChangedAt = passwordChangedAt;
        this.failedAttempts = failedAttempts;
        this.lockedUntil = lockedUntil;
    }

    public static UserPassword create(ShardedId userId, String passwordHash, Instant at) {
        return new UserPassword(userId, passwordHash, at, 0, null);
    }

    public static UserPassword reconstitute(ShardedId userId, String passwordHash, Instant passwordChangedAt,
                                            int failedAttempts, Instant lockedUntil) {
        return new UserPassword(userId, passwordHash, passwordChangedAt, failedAttempts, lockedUntil);
    }

    public void change(String newPasswordHash, Instant at) {
        this.passwordHash = newPasswordHash;
        this.passwordChangedAt = at;
        this.failedAttempts = 0;
        this.lockedUntil = null;
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void recordFailure(int maxAttempts, Duration lockDuration, Instant now) {
        this.failedAttempts++;
        if (failedAttempts >= maxAttempts) {
            this.lockedUntil = now.plus(lockDuration);
            this.failedAttempts = 0;
        }
    }

    public void recordSuccess() {
        this.failedAttempts = 0;
        this.lockedUntil = null;
    }
}