package com.ovelin.mall.auth.domain.model.valueobject;

import com.ovelin.mall.common.exception.global.InvalidArgumentException;
import lombok.Getter;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * (?=.*[A-Za-z])：至少包含一个英文字母。
 * (?=.*\\d)：至少包含一个数字。
 * .{8,64}：长度为 8～64 个 Java 正则字符，默认不包含换行符。
 * ^ 和 $：约束整个输入。
 */
@Getter
public class Password {
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,64}$");

    private final String value;

    private Password(String value) {
        this.value = value;
    }

    public static Password from(String value) {
        if (Objects.isNull(value) || value.isBlank()) {
            throw new InvalidArgumentException("密码不能为空");
        }
        if (!PASSWORD_PATTERN.matcher(value).matches()) {
            throw new InvalidArgumentException("密码必须为 8～64 位，且至少包含字母和数字");
        }
        return new Password(value);
    }

    public static Password reconstitute(String value) {
        return new Password(value);
    }
}