package com.ovelin.mall.user.infrastructure.persistence.converter;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.user.domain.model.aggregate.User;
import com.ovelin.mall.user.domain.model.enums.Gender;
import com.ovelin.mall.user.domain.model.enums.RegisterSource;
import com.ovelin.mall.user.domain.model.enums.UserStatus;
import com.ovelin.mall.user.domain.model.valueobject.Avatar;
import com.ovelin.mall.user.domain.model.valueobject.Birthday;
import com.ovelin.mall.user.infrastructure.persistence.po.UserPO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

// JUnit 的 @DisplayName 已 import，同名的值对象 DisplayName 只能写全限定名（见 name() 辅助方法）
@DisplayName("UserPOConverter")
class UserPOConverterTest {

    private static final long ID = 1001L;
    private static final String REGISTER_SOURCE = RegisterSource.ANDROID.code(); // 按 RegisterSource 实际 code 调整
    private static final String AVATAR_URL = "https://img.example.com/a.png";
    private static final Instant CREATED = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant UPDATED = Instant.parse("2026-10-05T00:00:00Z");
    private static final Instant DEREGISTERED_AT = Instant.parse("2026-10-09T08:00:00Z");

    private static com.ovelin.mall.user.domain.model.valueobject.DisplayName name(String value) {
        return com.ovelin.mall.user.domain.model.valueobject.DisplayName.from(value);
    }

    /** 一个字段齐全的 ACTIVE PO。 */
    private UserPO fullPO() {
        UserPO po = new UserPO();
        po.setId(ID);
        po.setDisplayName("Tom");
        po.setAvatarUrl(AVATAR_URL);
        po.setGender(1);
        po.setBirthYear(1995);
        po.setBirthMonth(6);
        po.setBirthDay(15);
        po.setStatus(UserStatus.ACTIVE.getValue());
        po.setRegisterSource(REGISTER_SOURCE);
        po.setCreatedAt(CREATED);
        po.setUpdatedAt(UPDATED);
        return po;
    }

    private User newUser() {
        return User.register(ShardedId.fromId(ID), name("Tom"), RegisterSource.fromCode(REGISTER_SOURCE));
    }

    @Nested
    @DisplayName("PO -> 聚合 toUser(po)")
    class ToUser {

        @Test
        @DisplayName("po 为 null 返回 null")
        void nullPoReturnsNull() {
            assertThat(UserPOConverter.toUser(null)).isNull();
        }

        @Test
        @DisplayName("字段齐全时全部还原")
        void restoresAllFields() {
            User user = UserPOConverter.toUser(fullPO());

            assertThat(user.getId().value()).isEqualTo(ID);
            assertThat(user.getDisplayName().value()).isEqualTo("Tom");
            assertThat(user.getRegisterSource().code()).isEqualTo(REGISTER_SOURCE);
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.getAvatar().getUrl()).isEqualTo(AVATAR_URL);
            assertThat(user.getGender().getCode()).isEqualTo(1);
            assertThat(user.getBirthday().getYear()).isEqualTo(1995);
            assertThat(user.getBirthday().getMonth()).isEqualTo(6);
            assertThat(user.getBirthday().getDay()).isEqualTo(15);
            assertThat(user.getCreatedAt()).isEqualTo(CREATED);
            assertThat(user.getUpdatedAt()).isEqualTo(UPDATED);
            assertThat(user.getDeregisteredAt()).isNull();
        }

        @ParameterizedTest(name = "状态 {0}")
        @EnumSource(UserStatus.class)
        @DisplayName("每种状态都能还原")
        void restoresEveryStatus(UserStatus status) {
            UserPO po = fullPO();
            po.setStatus(status.getValue());

            assertThat(UserPOConverter.toUser(po).getStatus()).isEqualTo(status);
        }

        @Test
        @DisplayName("可选字段为空：头像、生日为 null，性别回落为未知")
        void optionalFieldsAbsent() {
            UserPO po = fullPO();
            po.setAvatarUrl(null);
            po.setGender(null);
            po.setBirthYear(null);
            po.setBirthMonth(null);
            po.setBirthDay(null);

            User user = UserPOConverter.toUser(po);

            assertThat(user.getAvatar()).isNull();
            assertThat(user.getBirthday()).isNull();
            assertThat(user.getGender()).isEqualTo(Gender.UnKnown);
        }

        @Test
        @DisplayName("生日只有月日、没有年份时，生日仍然存在且年份为 null")
        void birthdayWithoutYear() {
            UserPO po = fullPO();
            po.setBirthYear(null);

            User user = UserPOConverter.toUser(po);

            assertThat(user.getBirthday()).isNotNull();
            assertThat(user.getBirthday().getYear()).isNull();
            assertThat(user.getBirthday().getMonth()).isEqualTo(6);
            assertThat(user.getBirthday().getDay()).isEqualTo(15);
        }

        @Test
        @DisplayName("月或日缺失时不构造生日")
        void birthdayNeedsMonthAndDay() {
            UserPO noMonth = fullPO();
            noMonth.setBirthMonth(null);
            UserPO noDay = fullPO();
            noDay.setBirthDay(null);

            assertThat(UserPOConverter.toUser(noMonth).getBirthday()).isNull();
            assertThat(UserPOConverter.toUser(noDay).getBirthday()).isNull();
        }

        @Test
        @DisplayName("已注销的墓碑行：状态、注销时间被还原，个人信息为空")
        void restoresTombstone() {
            UserPO po = fullPO();
            po.setStatus(UserStatus.DELETED.getValue());
            po.setAvatarUrl(null);
            po.setBirthYear(null);
            po.setBirthMonth(null);
            po.setBirthDay(null);
            po.setGender(Gender.UnKnown.getCode());
            po.setDeregisteredAt(DEREGISTERED_AT);

            User user = UserPOConverter.toUser(po);

            assertThat(user.isDeregistered()).isTrue();
            assertThat(user.getDeregisteredAt()).isEqualTo(DEREGISTERED_AT);
            assertThat(user.getAvatar()).isNull();
            assertThat(user.getBirthday()).isNull();
        }
    }

    @Nested
    @DisplayName("聚合 -> PO toPO(user)")
    class ToPO {

        @Test
        @DisplayName("新注册用户：必填项有值，可选字段为 null，状态 ACTIVE，性别未知")
        void newlyRegisteredUser() {
            UserPO po = UserPOConverter.toPO(newUser());

            assertThat(po.getId()).isEqualTo(ID);
            assertThat(po.getDisplayName()).isEqualTo("Tom");
            assertThat(po.getRegisterSource()).isEqualTo(REGISTER_SOURCE);
            assertThat(po.getStatus()).isEqualTo(UserStatus.ACTIVE.getValue());
            assertThat(po.getGender()).isEqualTo(Gender.UnKnown.getCode());
            assertThat(po.getAvatarUrl()).isNull();
            assertThat(po.getBirthYear()).isNull();
            assertThat(po.getBirthMonth()).isNull();
            assertThat(po.getBirthDay()).isNull();
            assertThat(po.getDeregisteredAt()).isNull();
        }

        @Test
        @DisplayName("资料齐全的用户：头像、性别、生日都写入")
        void fullProfile() {
            User user = newUser();
            user.changeAvatar(Avatar.from(AVATAR_URL));
            user.changeGender(Gender.fromCode(1));
            user.changeBirthday(Birthday.of(1995, 6, 15));

            UserPO po = UserPOConverter.toPO(user);

            assertThat(po.getAvatarUrl()).isEqualTo(AVATAR_URL);
            assertThat(po.getGender()).isEqualTo(1);
            assertThat(po.getBirthYear()).isEqualTo(1995);
            assertThat(po.getBirthMonth()).isEqualTo(6);
            assertThat(po.getBirthDay()).isEqualTo(15);
        }

        @Test
        @DisplayName("生日没有年份：年份列为 null，月日正常写入")
        void birthdayWithoutYear() {
            User user = newUser();
            user.changeBirthday(Birthday.of(null, 6, 15));

            UserPO po = UserPOConverter.toPO(user);

            assertThat(po.getBirthYear()).isNull();
            assertThat(po.getBirthMonth()).isEqualTo(6);
            assertThat(po.getBirthDay()).isEqualTo(15);
        }

        @Test
        @DisplayName("清除生日和头像后，对应列为 null")
        void clearedProfileBecomesNull() {
            User user = newUser();
            user.changeAvatar(Avatar.from(AVATAR_URL));
            user.changeBirthday(Birthday.of(1995, 6, 15));
            user.changeAvatar(null);
            user.changeBirthday(null);

            UserPO po = UserPOConverter.toPO(user);

            assertThat(po.getAvatarUrl()).isNull();
            assertThat(po.getBirthYear()).isNull();
            assertThat(po.getBirthMonth()).isNull();
            assertThat(po.getBirthDay()).isNull();
        }

        @Test
        @DisplayName("注销后的用户：状态、注销时间写入，个人信息列为 null，性别未知")
        void deregisteredUser() {
            User user = newUser();
            user.changeAvatar(Avatar.from(AVATAR_URL));
            user.changeBirthday(Birthday.of(1995, 6, 15));
            user.deregister(DEREGISTERED_AT);

            UserPO po = UserPOConverter.toPO(user);

            assertThat(po.getStatus()).isEqualTo(UserStatus.DELETED.getValue());
            assertThat(po.getDeregisteredAt()).isEqualTo(DEREGISTERED_AT);
            assertThat(po.getAvatarUrl()).isNull();
            assertThat(po.getBirthYear()).isNull();
            assertThat(po.getBirthMonth()).isNull();
            assertThat(po.getBirthDay()).isNull();
            assertThat(po.getGender()).isEqualTo(Gender.UnKnown.getCode());
        }

        @Test
        @DisplayName("即使聚合带有 createdAt / updatedAt，也不写进 PO")
        void neverWritesTimestamps() {
            User loaded = UserPOConverter.toUser(fullPO()); // 带 createdAt / updatedAt

            UserPO po = UserPOConverter.toPO(loaded);

            assertThat(loaded.getCreatedAt()).isEqualTo(CREATED);
            assertThat(po.getCreatedAt()).isNull();
            assertThat(po.getUpdatedAt()).isNull();
        }
    }

    @Test
    @DisplayName("往返：PO -> 聚合 -> PO，业务字段保持不变")
    void roundTrip() {
        UserPO original = fullPO();

        UserPO result = UserPOConverter.toPO(UserPOConverter.toUser(original));

        assertThat(result).usingRecursiveComparison()
                .ignoringFields("createdAt", "updatedAt")
                .isEqualTo(original);
    }
}