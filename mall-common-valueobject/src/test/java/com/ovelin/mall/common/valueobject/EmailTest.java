package com.ovelin.mall.common.valueobject;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class EmailTest {

    @Nested
    @DisplayName("from() 非法输入")
    class FromInvalid{

        @ParameterizedTest
        @ValueSource(strings = {"andy@","andy@q","andy@.q","andy@b.","andy@b.q","andy@b.q0","andy@.qq","andy@b.1q"})
        void shouldThrowException(String email){
            assertThatThrownBy(()->Email.from(email)).isInstanceOf(IllegalArgumentException.class);
        }
    }
    @Nested
    @DisplayName("from() 合法输入")
    class FromValid {
        @ParameterizedTest
        @ValueSource(strings = {"andy@b.qq", "y@b.qq", "andy@2.qq", "12@2.qq"})
        void shouldAccept(String email){
            assertThat(Email.from(email).getValue()).isEqualTo(email);
        }
        @ParameterizedTest
        @ValueSource(strings = {"andy@Example.com","ANDY@Example.com","andy@example.Com"})
        void shouldAcceptAndNormalized(String email){
            assertThat(Email.from(email).getValue()).isEqualTo(email.toLowerCase());
        }
    }

}
