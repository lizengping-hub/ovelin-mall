package com.ovelin.mall.auth.domain.model.valueobject;


import lombok.Getter;

import java.util.regex.Pattern;

/**
 * 本地部分（@前）：字母、数字、.、_、%、+、-
 * 必须有且仅有一个 @
 * 域名部分：字母、数字、.、-，且必须包含至少一个点
 * 顶级域名（最后一段）至少 2 个字母（排除 test@abc.1 这种）
 * @param value
 */
@Getter
public class Email {
    private final String value;
    private static final int MAX_LENGTH = 254; // RFC 5321 对邮箱总长度的限制

    // 宽松但实用的正则：本地部分 + @ + 域名(至少一个点，末尾是字母)
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");
    private Email(String value) {
        this.value = value;
    }
    public static Email from(String value) {
        String normalized = validate(value);
        return new Email(normalized);
    }
    private static String validate(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Email too long (max " + MAX_LENGTH + " chars)");
        }
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
        // 建议统一转小写存储，避免 Foo@Bar.com 和 foo@bar.com 被当成两个不同用户
        return value.toLowerCase();
    }
    public static Email reconstitute(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
        return new Email(value);
    }
}
