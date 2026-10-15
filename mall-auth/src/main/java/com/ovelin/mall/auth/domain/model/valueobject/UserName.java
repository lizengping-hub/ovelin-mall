package com.ovelin.mall.auth.domain.model.valueobject;

import com.ovelin.mall.common.exception.global.InvalidArgumentException;
import lombok.Getter;

import java.util.regex.Pattern;

/**
 * \p{L} 匹配任意语言的字母（包括中文），比 [a-zA-Z] 更通用；
 * 上限长度按你的实际业务（数据库字段长度等）调整；
 * 如果是显示昵称，是否要禁止纯空格、连续特殊字符、敏感词等，可以再加规则；
 * 如果这个 UserName 其实就是要给电话号码复用的字段名起错了，那就直接删掉这个类型，用 PhoneNumber 就行——但看字段名和"非登录名"的说明，更像是被误粘贴了不相关的正则，建议按用途重新定义规则。
 */
@Getter
public final class UserName {

    private static final int MAX_LENGTH = 50;
    private static final Pattern PATTERN = Pattern.compile("^[\\p{L}0-9_\\-. ]+$");
    private static final UserName DEREGISTERED_DISPLAY_NAME = new UserName("Deregistered User");
    private final String value;

    private UserName(String value) {
        this.value = value;
    }

    public static UserName deregistered(){
        return DEREGISTERED_DISPLAY_NAME;
    }

    /** 新建/修改用户名：严格校验，并去掉首尾空格 */
    public static UserName from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidArgumentException("Display name cannot be null or blank");
        }
        String value = raw.trim();
        if (value.length() > MAX_LENGTH) {
            throw new InvalidArgumentException("Display name too long (max " + MAX_LENGTH + " chars)");
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new InvalidArgumentException("Invalid Display name format");
        }
        return new UserName(value);
    }

    /** 从持久层还原：不做格式校验，兼容历史数据 */
    public static UserName reconstitute(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidArgumentException("Username cannot be null or blank");
        }
        return new UserName(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof UserName other && value.equals(other.value));
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}