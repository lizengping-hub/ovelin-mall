package com.ovelin.mall.auth.domain.model.enums;

import com.ovelin.mall.common.exception.global.InvalidArgumentException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum IdentityLookupStatus {

    RESERVED(0),
    ACTIVE(1),
    RELEASING(2);


    private final int code;

    public static IdentityLookupStatus fromCode(int code){
        for (IdentityLookupStatus status: values()){
            if (status.getCode() == code){
                return status;
            }
        }
        throw new InvalidArgumentException("未知的身份状态: " + code);
    }
}
