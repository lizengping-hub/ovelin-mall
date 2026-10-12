package com.ovelin.mall.user.domain.model.enums;

public enum RegisterSource {
    UNKNOWN("unknown"),
    WEB("web"),
    IOS("ios"),
    ANDROID("android"),
    MINI_PROGRAM("mini_program"),
    THIRD_PARTY("third_party");

    private final String code;

    RegisterSource(String code) { this.code = code; }

    public String code() { return code; }

    public static RegisterSource fromCode(String code) {
        for (RegisterSource s : values()) {
            if (s.code.equals(code)) return s;
        }
        return UNKNOWN;
    }
}