package com.ovelin.mall.user.domain.model.enums;

import com.ovelin.mall.common.exception.global.InvalidArgumentException;
import lombok.Getter;

@Getter
public enum Gender {
    UnKnown(0, "未知"),
    Male(1, "男"),
    Female(2, "女");

    private final int code;
    private final String description;

    Gender(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static Gender fromCode(int code) {
        for (Gender gender : Gender.values()) {
            if (gender.code == code) {
                return gender;
            }
        }
        throw new InvalidArgumentException("Invalid code for Gender: " + code);
    }


}
