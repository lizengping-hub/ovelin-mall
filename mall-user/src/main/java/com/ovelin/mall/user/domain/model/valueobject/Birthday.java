package com.ovelin.mall.user.domain.model.valueobject;

import com.ovelin.mall.common.exception.global.InvalidArgumentException;
import lombok.Getter;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.Period;
import java.time.Year;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * 生日值对象：year 可空（用户可不填出生年），month / day 必填。
 * 需要 Java 16+（record）。
 */
@Getter
public class Birthday {

    private final Integer year;
    private final int month;
    private final int day;

    private static final int MIN_YEAR = 1900;

    private Birthday(Integer year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    private static void validate(Integer year, int month, int day) {
        try {
            MonthDay.of(month, day); // 校验月日组合，允许 02-29
            if (year != null) {
                if (year < MIN_YEAR) {
                    throw new InvalidArgumentException("birth year too early: " + year);
                }
                LocalDate.of(year, month, day); // 校验带年份的组合，如平年 02-29 会抛异常
            }
        } catch (DateTimeException e) {
            throw new InvalidArgumentException("invalid birthday: " + year + "-" + month + "-" + day+ ", message: "+ e.getMessage());
        }
    }

    /** 完整生日 */
    public static Birthday from(LocalDate value) {
        return from(value, ZoneId.systemDefault());
    }

    /** 完整生日，显式指定"今天"所在时区，用于未来日期校验 */
    public static Birthday from(LocalDate value, ZoneId zone) {
        if (value == null) {
            throw new InvalidArgumentException("birthday must not be null");
        }
        if (value.isAfter(LocalDate.now(zone))) {
            throw new InvalidArgumentException("birthday must not be in the future: " + value);
        }
        return new Birthday(value.getYear(), value.getMonthValue(), value.getDayOfMonth());
    }

    /** 只有月日 */
    public static Birthday ofMonthDay(int month, int day) {
        return of(null, month, day);
    }

    /** 从 PO 还原（year 可为 null） */
    public static Birthday of(Integer year, int month, int day) {
        validate(year, month, day);
        return new Birthday(year, month, day);
    }

    /** 全空 -> empty；其余走统一校验 */
    public static Optional<Birthday> ofNullable(Integer year, Integer month, Integer day) {
        if (month == null && day == null) {
            if (year != null) throw new InvalidArgumentException("year without month/day");
            return Optional.empty();
        }
        if (month == null || day == null) {
            throw new InvalidArgumentException("month and day must be provided together");
        }
        return Optional.of(of(year, month, day));
    }
    public static Optional<Birthday> reconstituteNullable(Integer year, Integer month, Integer day) {
        if (month == null || day == null){
            return Optional.empty();
        }
        return Optional.of(new Birthday(year, month, day));
    }
    public static Birthday reconstitute(Integer year, int month, int day) {
        return new Birthday(year, month, day);
    }
    public boolean hasYear() {
        return year != null;
    }

    /** 未填年份时返回 empty */
    public OptionalInt age(LocalDate today) {
        if (year == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(Period.between(LocalDate.of(year, month, day), today).getYears());
    }

    /** 今天是否过生日；平年的 02-29 按 02-28 处理（业务规则，可按需改成 03-01） */
    public boolean isBirthdayOn(LocalDate date) {
        if (month == 2 && day == 29 && !Year.isLeap(date.getYear())) {
            return date.getMonthValue() == 2 && date.getDayOfMonth() == 28;
        }
        return date.getMonthValue() == month && date.getDayOfMonth() == day;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Birthday other)) return false;
        return Objects.equals(year, other.year) && month == other.month && day == other.day;
    }

    @Override
    public int hashCode() {
        return Objects.hash(year, month, day);
    }

    @Override
    public String toString() {
        return "Birthday{" +
                "year=" + year +
                ", month=" + month +
                ", day=" + day +
                '}';
    }
}