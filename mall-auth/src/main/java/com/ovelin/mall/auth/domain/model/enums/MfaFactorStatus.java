package com.ovelin.mall.auth.domain.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MfaFactorStatus {
    PENDING(0),
    ENABLED(1),
    DISABLED(2);

    private final int code;

    public static MfaFactorStatus fromCode(int code) {
        for (MfaFactorStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的因子状态: " + code);
    }
}