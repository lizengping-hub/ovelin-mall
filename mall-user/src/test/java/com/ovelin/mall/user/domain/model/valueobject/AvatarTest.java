package com.ovelin.mall.user.domain.model.valueobject;

import com.ovelin.mall.common.exception.global.InvalidArgumentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvatarTest {

    private static final String VALID_URL = "https://cdn.ovelin.com/avatar/1.png";

    @Nested
    @DisplayName("from() 合法输入")
    class FromValid {

        @Test
        void shouldCreateWithHttps() {
            Avatar avatar = Avatar.from(VALID_URL);
            assertThat(avatar.getUrl()).isEqualTo(VALID_URL);
        }

        @Test
        void shouldCreateWithHttp() {
            assertThat(Avatar.from("http://cdn.ovelin.com/a.jpg").getUrl())
                    .isEqualTo("http://cdn.ovelin.com/a.jpg");
        }

        @ParameterizedTest
        @ValueSource(strings = {".jpg", ".jpeg", ".png", ".webp", ".gif"})
        void shouldAcceptAllowedExtensions(String ext) {
            assertThat(Avatar.from("https://cdn.ovelin.com/a" + ext)).isNotNull();
        }

        @Test
        void shouldBeCaseInsensitiveForSchemeAndExtension() {
            assertThat(Avatar.from("HTTPS://cdn.ovelin.com/A.PNG").getUrl()).isEqualTo("https://cdn.ovelin.com/a.png");
        }

        @Test
        void shouldAcceptQueryString() {
            assertThat(Avatar.from("https://cdn.ovelin.com/a.png?v=2")).isNotNull();
        }

        @Test
        void shouldAcceptMaxLength() {
            // "https://a.com/" (14) + 482 个 a + ".png" (4) = 500
            String url = "https://a.com/" + "a".repeat(482) + ".png";
            assertThat(url).hasSize(500);
            assertThat(Avatar.from(url).getUrl()).isEqualTo(url);
        }
        @Test
        void shouldStoreTrimmedUrl() {
            assertThat(Avatar.from("  " + VALID_URL + "  ").getUrl()).isEqualTo(VALID_URL);
        }
    }

    @Nested
    @DisplayName("from() 非法输入")
    class FromInvalid {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        void shouldRejectNullOrBlank(String url) {
            assertThatThrownBy(() -> Avatar.from(url))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasMessageContaining("cannot be null or blank");
        }

        @Test
        void shouldRejectTooLong() {
            String url = "https://a.com/" + "a".repeat(483) + ".png"; // 501
            assertThatThrownBy(() -> Avatar.from(url))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasMessageContaining("too long");
        }

        @Test
        void shouldRejectInvalidSyntax() {
            assertThatThrownBy(() -> Avatar.from("https://cdn.ovelin.com/a b.png"))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasMessageContaining("Invalid avatar url");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "ftp://cdn.ovelin.com/a.png",
                "file:///tmp/a.png",
                "javascript:alert(1).png",
                "cdn.ovelin.com/a.png",
                "//cdn.ovelin.com/a.png",
        })
        void shouldRejectNonHttpScheme(String url) {
            assertThatThrownBy(() -> Avatar.from(url))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasMessageContaining("must be http/https");
        }

        @Test
        void shouldRejectMissingHost() {
            assertThatThrownBy(() -> Avatar.from("https:///a.png"))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasMessageContaining("valid host");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "https://cdn.ovelin.com/a.txt",
                "https://cdn.ovelin.com/a",
                "https://cdn.ovelin.com/a.svg",
                "https://cdn.ovelin.com/a.png.exe",
                "https://evil.com/x.html?f=a.png"
        })
        void shouldRejectNonImageExtension(String url) {
            assertThatThrownBy(() -> Avatar.from(url))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasMessageContaining("must point to an image file");
        }
    }

    @Nested
    @DisplayName("reconstitute()")
    class Reconstitute {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t"})
        void shouldReturnNullForNullOrBlank(String url) {
            assertThatThrownBy(()->Avatar.reconstitute(url)).isInstanceOf(InvalidArgumentException.class).hasMessageContaining("cannot be null or blank");
        }

        @Test
        void shouldNotValidateStoredValue() {
            // 历史数据不做校验，保证能从库里还原
            Avatar avatar = Avatar.reconstitute("ftp://legacy/avatar");
            assertThat(avatar).isNotNull();
            assertThat(avatar.getUrl()).isEqualTo("ftp://legacy/avatar");
        }
    }

    @Test
    void defaultAvatarShouldBeValid() {
        Avatar def = Avatar.defaultAvatar();
        assertThat(def.getUrl()).isEqualTo("https://cdn.ovelin.com/default-avatar.png");
        // 默认头像本身应能通过 from() 的校验
        assertThat(Avatar.from(def.getUrl())).isEqualTo(def);
    }

    @Nested
    @DisplayName("equals / hashCode / toString")
    class ValueSemantics {

        @Test
        void sameUrlShouldBeEqual() {
            Avatar a = Avatar.from(VALID_URL);
            Avatar b = Avatar.from(VALID_URL);
            assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        }

        @Test
        void sameInstanceShouldBeEqual() {
            Avatar a = Avatar.from(VALID_URL);
            assertThat(a).isEqualTo(a);
        }

        @Test
        void differentUrlShouldNotBeEqual() {
            assertThat(Avatar.from(VALID_URL))
                    .isNotEqualTo(Avatar.from("https://cdn.ovelin.com/avatar/2.png"));
        }

        @Test
        void shouldNotEqualNullOrOtherType() {
            Avatar a = Avatar.from(VALID_URL);
            assertThat(a).isNotEqualTo(null);
            assertThat(a).isNotEqualTo(VALID_URL);
        }

        @Test
        void fromAndReconstituteShouldBeEqual() {
            assertThat(Avatar.from(VALID_URL)).isEqualTo(Avatar.reconstitute(VALID_URL));
        }

        @Test
        void toStringShouldContainUrl() {
            assertThat(Avatar.from(VALID_URL).toString())
                    .isEqualTo("Avatar{url='" + VALID_URL + "'}");
        }
    }
}