package com.ovelin.mall.auth.domain.model.aggregate;

import com.ovelin.mall.auth.domain.model.valueobject.EncodedPassword;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Getter
public class UserPassword {

    private final ShardedId userId;
    private EncodedPassword encodedPassword;
    private Instant passwordChangedAt;
    private int failedAttempts;
    private Instant lockedUntil;

    private UserPassword(ShardedId userId, EncodedPassword encodedPassword, Instant passwordChangedAt,
                         int failedAttempts, Instant lockedUntil) {
        this.userId = userId;
        this.encodedPassword = encodedPassword;
        this.passwordChangedAt = passwordChangedAt;
        this.failedAttempts = failedAttempts;
        this.lockedUntil = lockedUntil;
    }

    public static UserPassword create(@NotNull ShardedId userId, @NotNull EncodedPassword passwordHash, @NotNull Instant at) {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(passwordHash);
        Objects.requireNonNull(at);
        return new UserPassword(userId, passwordHash, at, 0, null);
    }

    public static UserPassword reconstitute(ShardedId userId, String encodedPassword, Instant passwordChangedAt,
                                            int failedAttempts, Instant lockedUntil) {
        return new UserPassword(userId, new EncodedPassword(encodedPassword), passwordChangedAt, failedAttempts, lockedUntil);
    }

    public void change(EncodedPassword encodedPassword, Instant at) {
        this.encodedPassword = encodedPassword;
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