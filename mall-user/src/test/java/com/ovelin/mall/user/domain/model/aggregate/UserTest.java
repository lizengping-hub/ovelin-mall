package com.ovelin.mall.user.domain.model.aggregate;


import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.user.domain.exception.InvalidUserStatusException;
import com.ovelin.mall.user.domain.model.enums.Gender;
import com.ovelin.mall.user.domain.model.enums.RegisterSource;
import com.ovelin.mall.user.domain.model.enums.UserStatus;
import com.ovelin.mall.user.domain.model.valueobject.Avatar;
import com.ovelin.mall.user.domain.model.valueobject.Birthday;
import com.ovelin.mall.user.domain.model.valueobject.UserSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

// 注意：JUnit 的 @DisplayName 已 import，同包的值对象 DisplayName 必须写全限定名
@DisplayName("User 聚合")
class UserTest {

    private final ShardedId id = ShardedId.of(100, 1000);
    private final com.ovelin.mall.user.domain.model.valueobject.DisplayName name = com.ovelin.mall.user.domain.model.valueobject.DisplayName.from("Tom");
    private final RegisterSource source = RegisterSource.UNKNOWN;

    /** 用 reconstitute 构造任意状态的用户，不依赖 setter。 */
    private User userWith(UserStatus status) {
        return User.reconstitute(new UserSnapshot(
                id, name, source, status,
                mock(Avatar.class), Gender.UnKnown, mock(Birthday.class), null, null, null));
    }

    @Nested
    @DisplayName("注册 register")
    class Register {

        @Test
        @DisplayName("新用户默认 ACTIVE、性别未知，时间由数据库生成")
        void defaultsAfterRegister() {
            User user = User.register(id, name, source);

            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.getGender()).isEqualTo(Gender.UnKnown);
            assertThat(user.getAvatar()).isNull();
            assertThat(user.getBirthday()).isNull();
            assertThat(user.getCreatedAt()).isNull();
            assertThat(user.getUpdatedAt()).isNull();
            assertThat(user.getDeregisteredAt()).isNull();
        }

        @Test
        @DisplayName("必填项为 null 时抛出 NPE")
        void requiredFieldsMustNotBeNull() {
            assertThatThrownBy(() -> User.register(null, name, source))
                    .isInstanceOf(NullPointerException.class).hasMessage("User id must not be null");
            assertThatThrownBy(() -> User.register(id, null, source))
                    .isInstanceOf(NullPointerException.class).hasMessage("User displayName must not be null");
            assertThatThrownBy(() -> User.register(id, name, null))
                    .isInstanceOf(NullPointerException.class).hasMessage("User registerSource must not be null");
        }
    }

    @Nested
    @DisplayName("还原 reconstitute")
    class Reconstitute {

        @Test
        @DisplayName("gender 为空时回落为未知，其余字段原样还原")
        void genderFallsBackToUnknown() {
            User user = User.reconstitute(new UserSnapshot(
                    id, name, source, UserStatus.DISABLED, null, null, null, null, null, null));

            assertThat(user.getStatus()).isEqualTo(UserStatus.DISABLED);
            assertThat(user.getGender()).isEqualTo(Gender.UnKnown);
        }

        @ParameterizedTest(name = "状态 {0}")
        @EnumSource(UserStatus.class)
        @DisplayName("任意状态都能还原")
        void canRestoreAnyStatus(UserStatus status) {
            assertThat(userWith(status).getStatus()).isEqualTo(status);
        }
    }

    @Nested
    @DisplayName("禁用 / 解禁")
    class BanUnban {

        @Test
        @DisplayName("ACTIVE 用户可以被禁用")
        void activeCanBeBanned() {
            User user = userWith(UserStatus.ACTIVE);

            user.ban();

            assertThat(user.getStatus()).isEqualTo(UserStatus.DISABLED);
        }

        @ParameterizedTest(name = "状态 {0}")
        @EnumSource(value = UserStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("非 ACTIVE 用户禁用时抛异常，状态不变")
        void nonActiveCannotBeBanned(UserStatus status) {
            User user = userWith(status);

            assertThatThrownBy(user::ban).isInstanceOf(InvalidUserStatusException.class);
            assertThat(user.getStatus()).isEqualTo(status);
        }

        @Test
        @DisplayName("DISABLED 用户可以被解禁")
        void disabledCanBeUnbanned() {
            User user = userWith(UserStatus.DISABLED);

            user.unban();

            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @ParameterizedTest(name = "状态 {0}")
        @EnumSource(value = UserStatus.class, names = "DISABLED", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("非 DISABLED 用户解禁时抛异常，状态不变")
        void nonDisabledCannotBeUnbanned(UserStatus status) {
            User user = userWith(status);

            assertThatThrownBy(user::unban).isInstanceOf(InvalidUserStatusException.class);
            assertThat(user.getStatus()).isEqualTo(status);
        }
    }

    @Nested
    @DisplayName("注销 deregister")
    class Deregister {

        private static final java.time.Instant AT = java.time.Instant.parse("2026-10-09T08:00:00Z");

        @ParameterizedTest(name = "状态 {0}")
        @EnumSource(value = UserStatus.class, names = "DELETED", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("ACTIVE 和 DISABLED 用户都可以注销")
        void activeAndDisabledCanDeregister(UserStatus status) {
            User user = userWith(status);

            user.deregister(AT);

            assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
            assertThat(user.isDeregistered()).isTrue();
        }

        @Test
        @DisplayName("注销时间取自传入的时间，而不是领域层自己取")
        void deregisteredAtComesFromCaller() {
            User user = userWith(UserStatus.ACTIVE);

            user.deregister(AT);

            assertThat(user.getDeregisteredAt()).isEqualTo(AT);
        }

        @Test
        @DisplayName("注销时间不能为 null，且失败时状态不变")
        void deregisterRequiresTime() {
            User user = userWith(UserStatus.ACTIVE);

            assertThatThrownBy(() -> user.deregister(null)).isInstanceOf(NullPointerException.class);
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.getDeregisteredAt()).isNull();
        }

        @Test
        @DisplayName("注销后个人信息被清除，只保留 id 和注册来源")
        void personalInfoIsErased() {
            User user = userWith(UserStatus.ACTIVE);

            user.deregister(AT);

            assertThat(user.getDisplayName()).isEqualTo(com.ovelin.mall.user.domain.model.valueobject.DisplayName.deregistered());
            assertThat(user.getAvatar()).isNull();
            assertThat(user.getBirthday()).isNull();
            assertThat(user.getGender()).isEqualTo(Gender.UnKnown);
            assertThat(user.getId()).isEqualTo(id);
            assertThat(user.getRegisterSource()).isEqualTo(source);
        }

        @Test
        @DisplayName("重复注销抛异常")
        void deregisterTwiceFails() {
            User user = userWith(UserStatus.DELETED);

            assertThatThrownBy(() -> user.deregister(AT)).isInstanceOf(InvalidUserStatusException.class);
            assertThat(user.getDeregisteredAt()).isNull();
        }
    }

    @Nested
    @DisplayName("修改资料 change*")
    class Changes {

        record Op(String name, Consumer<User> action, Predicate<User> changed) {
            @Override public String toString() { return name; }
        }

        private static final com.ovelin.mall.user.domain.model.valueobject.DisplayName newName = com.ovelin.mall.user.domain.model.valueobject.DisplayName.from("Tom");
        private static final Avatar avatar = Avatar.from("https://example.com/default.png");
        private static final Birthday birthday = Birthday.of(2000, 1,1);

        static Stream<Op> ops() {
            return Stream.of(
                    new Op("changeDisplayName", u -> u.changeDisplayName(newName), u -> u.getDisplayName() == newName),
                    new Op("changeAvatar", u -> u.changeAvatar(avatar), u -> u.getAvatar() == avatar),
                    new Op("changeGender", u -> u.changeGender(Gender.UnKnown), u -> true),
                    new Op("changeBirthday", u -> u.changeBirthday(birthday), u -> u.getBirthday() == birthday));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("ops")
        @DisplayName("ACTIVE 用户可以修改")
        void activeCanChange(Op op) {
            User user = userWith(UserStatus.ACTIVE);

            op.action().accept(user);

            assertThat(op.changed().test(user)).isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("ops")
        @DisplayName("DISABLED 用户不能修改")
        void disabledCannotChange(Op op) {
            User user = userWith(UserStatus.DISABLED);

            assertThatThrownBy(() -> op.action().accept(user)).isInstanceOf(InvalidUserStatusException.class);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("ops")
        @DisplayName("DELETED 用户不能修改")
        void deletedCannotChange(Op op) {
            User user = userWith(UserStatus.DELETED);

            assertThatThrownBy(() -> op.action().accept(user)).isInstanceOf(InvalidUserStatusException.class);
        }

        @Test
        @DisplayName("昵称和性别不允许置空")
        void displayNameAndGenderRejectNull() {
            User user = userWith(UserStatus.ACTIVE);

            assertThatThrownBy(() -> user.changeDisplayName(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> user.changeGender(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("头像和生日允许清除")
        void avatarAndBirthdayCanBeCleared() {
            User user = userWith(UserStatus.ACTIVE);

            user.changeAvatar(null);
            user.changeBirthday(null);

            assertThat(user.getAvatar()).isNull();
            assertThat(user.getBirthday()).isNull();
        }
    }
}
