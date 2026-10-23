package com.ovelin.mall.auth.domain.model.valueobject;

import com.ovelin.mall.common.exception.global.InvalidArgumentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PhoneNumberTest {
    @Nested
    @DisplayName("parseRawInput() 有效输入")
    class ParseRawInputValid{

        @ParameterizedTest
        @MethodSource("validInputs")
        void shouldAcceptAndFormat(String rawInput, String target) {
            PhoneNumber phoneNumber = PhoneNumber.parseRawInput(rawInput, "CN");
            assertThat(target).isEqualTo(phoneNumber.getE164());
        }

        static Stream<Arguments> validInputs() {
            return Stream.of(
                    Arguments.of("139-7876-8765","+8613978768765"),
                    Arguments.of("139 7876 8765","+8613978768765"),
                    Arguments.of("13978768765", "+8613978768765"),
                    Arguments.of("+86-139-7876-8765","+8613978768765"),
                    Arguments.of("86 139 7876 8765","+8613978768765"),
                    Arguments.of("+86 139 7876 8765","+8613978768765"),
                    Arguments.of("+86139-7876-8765","+8613978768765"),
                    Arguments.of("+86139 7876 8765","+8613978768765"),
                    Arguments.of("+8613978768765", "+8613978768765")
            );
        }
    }
    @Nested
    @DisplayName("parseRawInput() 无效输入")
    class ParseRawInputInvalid{
        @ParameterizedTest
        @ValueSource(strings = {"12345678901","+8612345678901","1397876876b","+13978768765","400-800-8888"})
        void shouldThrowException(String phone){
            assertThatThrownBy(()->PhoneNumber.parseRawInput(phone)).isInstanceOf(InvalidArgumentException.class);
        }
    }

}
