package com.ovelin.mall.auth.application.port;

import java.time.Duration;

public record PasswordPolicy(int maxFailedAttempts, Duration lockDuration) {
}