package com.ovelin.mall.auth.domain.model.valueobject;

import com.ovelin.mall.common.exception.global.InvalidArgumentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordTest {

    @Nested
    @DisplayName("from")
    class From {

        @Test
        @DisplayName("合法密码创建成功")
        void createsWhenValid() {
            Password password = Password.from("abc12345");

            assertThat(password).isNotNull();
            assertThat(password).hasFieldOrPropertyWithValue("value", "abc12345");
        }

        @Test
        @DisplayName("密码为 null 时抛出异常")
        void throwsWhenNull() {
            assertThatThrownBy(() -> Password.from(null))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasMessage("密码不能为空");
        }

        @Test
        @DisplayName("密码为空白时抛出异常")
        void throwsWhenBlank() {
            assertThatThrownBy(() -> Password.from("   "))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasMessage("密码不能为空");
        }

        @Test
        @DisplayName("密码长度小于 8 位时抛出异常")
        void throwsWhenTooShort() {
            assertInvalidPassword("abc1234");
        }

        @Test
        @DisplayName("密码长度超过 64 位时抛出异常")
        void throwsWhenTooLong() {
            assertInvalidPassword("a1" + "b".repeat(63));
        }

        @Test
        @DisplayName("密码长度为 8 位时创建成功")
        void createsWhenLengthIsEight() {
            assertThat(Password.from("abc12345")).isNotNull();
        }

        @Test
        @DisplayName("密码长度为 64 位时创建成功")
        void createsWhenLengthIsSixtyFour() {
            assertThat(Password.from("a1" + "b".repeat(62))).isNotNull();
        }

        @Test
        @DisplayName("密码不包含字母时抛出异常")
        void throwsWhenNoLetter() {
            assertInvalidPassword("12345678");
        }

        @Test
        @DisplayName("密码不包含数字时抛出异常")
        void throwsWhenNoDigit() {
            assertInvalidPassword("abcdefgh");
        }

        @Test
        @DisplayName("密码包含特殊字符时允许创建")
        void createsWhenContainsSpecialCharacters() {
            assertThat(Password.from("abc123!@")).isNotNull();
        }

        private void assertInvalidPassword(String value) {
            assertThatThrownBy(() -> Password.from(value))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasMessage("密码必须为 8～64 位，且至少包含字母和数字");
        }
    }

    @Nested
    @DisplayName("reconstitute")
    class Reconstitute {

        @Test
        @DisplayName("恢复已有密码时不执行格式校验")
        void restoresWithoutValidation() {
            Password password = Password.reconstitute("invalid");

            assertThat(password).isNotNull();
            assertThat(password).hasFieldOrPropertyWithValue("value", "invalid");
        }
    }
}