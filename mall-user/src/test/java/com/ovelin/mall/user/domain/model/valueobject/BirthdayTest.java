package com.ovelin.mall.user.domain.model.valueobject;

import com.ovelin.mall.user.domain.exception.InvalidArgumentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BirthdayTest {

    @Nested
    @DisplayName("from(LocalDate)")
    class From {

        @Test
        void splitsFullDateIntoYearMonthDay() {
            Birthday b = Birthday.from(LocalDate.of(1990, 5, 20));

            assertThat(b.getYear()).isEqualTo(1990);
            assertThat(b.getMonth()).isEqualTo(5);
            assertThat(b.getDay()).isEqualTo(20);
            assertThat(b.hasYear()).isTrue();
        }

        @Test
        void rejectsNullDate() {
            assertThatThrownBy(() -> Birthday.from(null))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasMessageContaining("must not be null");
        }

        @Test
        void rejectsFutureDate() {
            LocalDate tomorrow = LocalDate.now().plusDays(2); // +2 避免跨时区/跨零点的边界抖动

            assertThatThrownBy(() -> Birthday.from(tomorrow))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasMessageContaining("future");
        }

        @Test
        void acceptsToday() {
            ZoneId zone = ZoneId.of("UTC");
            LocalDate today = LocalDate.now(zone);

            assertThatCode(() -> Birthday.from(today, zone)).doesNotThrowAnyException();
        }

        @Test
        void zoneDeterminesWhichDateIsFuture() {
            // UTC+14 的"今天"，在 UTC-11 看来一定是未来
            ZoneId earliest = ZoneId.of("Pacific/Kiritimati");
            ZoneId latest = ZoneId.of("Pacific/Pago_Pago");
            LocalDate kiritimatiToday = LocalDate.now(earliest);

            assertThatCode(() -> Birthday.from(kiritimatiToday, earliest)).doesNotThrowAnyException();
            assertThatThrownBy(() -> Birthday.from(kiritimatiToday, latest))
                    .isInstanceOf(InvalidArgumentException.class);
        }
    }

    @Nested
    @DisplayName("ofMonthDay")
    class OfMonthDay {

        @Test
        void createsMonthDayOnlyBirthdayWithNullYear() {
            Birthday b = Birthday.ofMonthDay(12, 31);

            assertThat(b.getYear()).isNull();
            assertThat(b.getMonth()).isEqualTo(12);
            assertThat(b.getDay()).isEqualTo(31);
            assertThat(b.hasYear()).isFalse();
        }

        @Test
        void allowsFeb29WithoutYear() {
            assertThatCode(() -> Birthday.ofMonthDay(2, 29)).doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "{0}-{1} is invalid")
        @CsvSource({"0,1", "13,1", "1,0", "1,32", "2,30", "4,31", "6,31", "-1,10"})
        void rejectsInvalidMonthDay(int month, int day) {
            assertThatThrownBy(() -> Birthday.ofMonthDay(month, day))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasMessageContaining("invalid birthday");
        }
    }

    @Nested
    @DisplayName("of(year, month, day)")
    class Of {

        @Test
        void nullYearEqualsMonthDayOnly() {
            Birthday b = Birthday.of(null, 8, 8);

            assertThat(b).isEqualTo(Birthday.ofMonthDay(8, 8));
            assertThat(b.hasYear()).isFalse();
        }

        @Test
        void acceptsValidFullDate() {
            Birthday b = Birthday.of(1985, 3, 15);

            assertThat(b.getYear()).isEqualTo(1985);
        }

        @Test
        void acceptsMinYearBoundary() {
            assertThatCode(() -> Birthday.of(1900, 1, 1)).doesNotThrowAnyException();
        }

        @Test
        void rejectsYearBeforeMin() {
            assertThatThrownBy(() -> Birthday.of(1899, 12, 31))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasMessageContaining("too early");
        }

        @Test
        void acceptsFeb29InLeapYear() {
            assertThatCode(() -> Birthday.of(2000, 2, 29)).doesNotThrowAnyException();
            assertThatCode(() -> Birthday.of(2024, 2, 29)).doesNotThrowAnyException();
        }

        @Test
        void rejectsFeb29InNonLeapYear() {
            assertThatThrownBy(() -> Birthday.of(2023, 2, 29))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasMessageContaining("invalid birthday");
            assertThatThrownBy(() -> Birthday.of(1900, 2, 29)) // 1900 不是闰年
                    .isInstanceOf(InvalidArgumentException.class);
        }

        @Test
        void rejectsInvalidMonthDayWithYear() {
            assertThatThrownBy(() -> Birthday.of(1990, 13, 1))
                    .isInstanceOf(InvalidArgumentException.class);
            assertThatThrownBy(() -> Birthday.of(null, 2, 30))
                    .isInstanceOf(InvalidArgumentException.class);
        }
    }

    @Nested
    @DisplayName("reconstitute")
    class Reconstitute {

        @Test
        void restoresNullable() {
            Optional<Birthday> birthdayOptional = Birthday.reconstituteNullable(null, null, null);

            assertThat(birthdayOptional.isEmpty()).isTrue();
        }

        @Test
        void restoresWithoutValidation() {
            Birthday b = Birthday.reconstitute(null, 13, 40);

            assertThat(b.getYear()).isNull();
            assertThat(b.getMonth()).isEqualTo(13);
            assertThat(b.getDay()).isEqualTo(40);
        }

        @Test
        void validDataEqualsOfResult() {
            assertThat(Birthday.reconstitute(1999, 9, 9)).isEqualTo(Birthday.of(1999, 9, 9));
            assertThat(Birthday.reconstitute(null, 9, 9)).isEqualTo(Birthday.ofMonthDay(9, 9));
        }
    }

    @Nested
    @DisplayName("age")
    class Age {

        @Test
        void returnsEmptyWhenYearMissing() {
            assertThat(Birthday.ofMonthDay(5, 20).age(LocalDate.of(2025, 5, 20)))
                    .isEqualTo(OptionalInt.empty());
        }

        @ParameterizedTest(name = "born 1990-05-20, today {0} -> age {1}")
        @CsvSource({
                "2025-05-19, 34", // 生日前一天
                "2025-05-20, 35", // 生日当天
                "2025-05-21, 35", // 生日后一天
                "2025-01-01, 34",
                "2025-12-31, 35",
                "1990-05-20, 0"   // 出生当天
        })
        void calculatesAgeInFullYears(LocalDate today, int expected) {
            Birthday b = Birthday.of(1990, 5, 20);

            assertThat(b.age(today)).hasValue(expected);
        }

        @Test
        void leapDayBornAgeFollowsPeriodRules() {
            Birthday b = Birthday.of(2000, 2, 29);

            assertThat(b.age(LocalDate.of(2023, 2, 28))).hasValue(22);
            assertThat(b.age(LocalDate.of(2023, 3, 1))).hasValue(23);
            assertThat(b.age(LocalDate.of(2024, 2, 29))).hasValue(24);
        }
    }

    @Nested
    @DisplayName("isBirthdayOn")
    class IsBirthdayOn {

        @Test
        void matchesOnlyOnBirthdayForRegularDate() {
            Birthday b = Birthday.ofMonthDay(5, 20);

            assertThat(b.isBirthdayOn(LocalDate.of(2025, 5, 20))).isTrue();
            assertThat(b.isBirthdayOn(LocalDate.of(2025, 5, 19))).isFalse();
            assertThat(b.isBirthdayOn(LocalDate.of(2025, 5, 21))).isFalse();
            assertThat(b.isBirthdayOn(LocalDate.of(2025, 6, 20))).isFalse();
        }

        @Test
        void ignoresYearWhenMatching() {
            Birthday b = Birthday.of(1990, 5, 20);

            assertThat(b.isBirthdayOn(LocalDate.of(2030, 5, 20))).isTrue();
        }

        @Test
        void leapDayBirthdayMatchesFeb29InLeapYear() {
            Birthday b = Birthday.ofMonthDay(2, 29);

            assertThat(b.isBirthdayOn(LocalDate.of(2024, 2, 29))).isTrue();
            assertThat(b.isBirthdayOn(LocalDate.of(2024, 2, 28))).isFalse();
            assertThat(b.isBirthdayOn(LocalDate.of(2024, 3, 1))).isFalse();
        }

        @Test
        void leapDayBirthdayFallsOnFeb28InNonLeapYear() {
            Birthday b = Birthday.ofMonthDay(2, 29);

            assertThat(b.isBirthdayOn(LocalDate.of(2025, 2, 28))).isTrue();
            assertThat(b.isBirthdayOn(LocalDate.of(2025, 3, 1))).isFalse();
        }

        @Test
        void feb28BirthdayUnaffectedByLeapRule() {
            Birthday b = Birthday.ofMonthDay(2, 28);

            assertThat(b.isBirthdayOn(LocalDate.of(2025, 2, 28))).isTrue();
            assertThat(b.isBirthdayOn(LocalDate.of(2024, 2, 28))).isTrue();
        }
    }

    @Nested
    @DisplayName("equals / hashCode / toString")
    class ValueSemantics {

        @Test
        void sameValuesAreEqualWithSameHashCode() {
            Birthday a = Birthday.of(1990, 5, 20);
            Birthday b = Birthday.from(LocalDate.of(1990, 5, 20));

            assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        }

        @Test
        void bothWithoutYearAreEqual() {
            assertThat(Birthday.ofMonthDay(5, 20))
                    .isEqualTo(Birthday.ofMonthDay(5, 20))
                    .hasSameHashCodeAs(Birthday.ofMonthDay(5, 20));
        }

        @Test
        void differentFieldMeansNotEqual() {
            Birthday base = Birthday.of(1990, 5, 20);

            assertThat(base).isNotEqualTo(Birthday.of(1991, 5, 20));
            assertThat(base).isNotEqualTo(Birthday.of(1990, 6, 20));
            assertThat(base).isNotEqualTo(Birthday.of(1990, 5, 21));
            assertThat(base).isNotEqualTo(Birthday.ofMonthDay(5, 20)); // 有年 vs 无年
        }

        @Test
        void notEqualToNullOrOtherType() {
            Birthday b = Birthday.of(1990, 5, 20);

            assertThat(b).isNotEqualTo(null);
            assertThat(b).isNotEqualTo("1990-05-20");
            assertThat(b).isEqualTo(b);
        }

        @Test
        void toStringContainsFields() {
            assertThat(Birthday.of(1990, 5, 20).toString())
                    .isEqualTo("Birthday{year=1990, month=5, day=20}");
            assertThat(Birthday.ofMonthDay(5, 20).toString())
                    .isEqualTo("Birthday{year=null, month=5, day=20}");
        }
    }
}