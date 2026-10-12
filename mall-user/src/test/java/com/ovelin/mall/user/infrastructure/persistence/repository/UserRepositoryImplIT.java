package com.ovelin.mall.user.infrastructure.persistence.repository;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.user.TestApplication;
import com.ovelin.mall.user.domain.exception.InvalidUserStatusException;
import com.ovelin.mall.user.domain.model.aggregate.User;
import com.ovelin.mall.user.domain.model.enums.Gender;
import com.ovelin.mall.user.domain.model.enums.RegisterSource;
import com.ovelin.mall.user.domain.model.enums.UserStatus;
import com.ovelin.mall.user.domain.model.valueobject.Avatar;
import com.ovelin.mall.user.domain.model.valueobject.Birthday;
import com.ovelin.mall.user.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * UserRepository 集成测试：连接真实数据库，验证单元测试验证不了的部分——
 * 真实 SQL 的读写、数据库生成的时间、updateById 对 null 的处理、墓碑行可读。
 * 每个测试在事务内执行并回滚，互不影响。
 */
// JUnit 的 @DisplayName 已 import，同名的值对象 DisplayName 只能写全限定名（见 name() 辅助方法）
@SpringBootTest(classes = TestApplication.class)
@Transactional
@DisplayName("UserRepository 集成测试")
  class UserRepositoryImplIT {

    private static final String REGISTER_SOURCE = RegisterSource.ANDROID.code();
    private static final String AVATAR_URL = "https://img.example.com/a.png";
    /** 毫秒精度：列是 DATETIME(3)，用带纳秒的时间会在读回时被截断，造成无意义的失败 */
    private static final Instant DEREGISTERED_AT = Instant.parse("2026-10-09T08:00:00.123Z");

    private static final AtomicLong SEQ = new AtomicLong(9_000_000_000L);

    @Autowired
    UserRepository userRepository;

    private static ShardedId nextId() {
        return ShardedId.fromId(SEQ.incrementAndGet());
    }

    private static com.ovelin.mall.user.domain.model.valueobject.DisplayName name(String value) {
        return com.ovelin.mall.user.domain.model.valueobject.DisplayName.from(value);
    }

    private User newUser(ShardedId id) {
        return User.register(id, name("Tom"), RegisterSource.fromCode(REGISTER_SOURCE));
    }

    /** 插入并重新从数据库读出，拿到带数据库生成时间的聚合。 */
    private User addAndReload(User user) {
        userRepository.add(user);
        return userRepository.findById(user.getId());
    }

    @Nested
    @DisplayName("add + findById")
    class AddAndFind {

        @Test
        @DisplayName("新用户插入后能读回，createdAt / updatedAt 由数据库生成")
        void roundTripWithDbGeneratedTimestamps() {
            ShardedId id = nextId();
            User user = newUser(id);
            assertThat(user.getCreatedAt()).isNull(); // 插入前没有时间

            User loaded = addAndReload(user);

            assertThat(loaded).isNotNull();
            assertThat(loaded.getId().value()).isEqualTo(id.value());
            assertThat(loaded.getDisplayName().value()).isEqualTo("Tom");
            assertThat(loaded.getRegisterSource().code()).isEqualTo(REGISTER_SOURCE);
            assertThat(loaded.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(loaded.getGender()).isEqualTo(Gender.UnKnown);
            assertThat(loaded.getAvatar()).isNull();
            assertThat(loaded.getBirthday()).isNull();
            assertThat(loaded.getDeregisteredAt()).isNull();
            assertThat(loaded.getCreatedAt()).isNotNull();
            assertThat(loaded.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("资料齐全的用户：头像、性别、完整生日都能存取")
        void fullProfile() {
            User user = newUser(nextId());
            user.changeAvatar(Avatar.from(AVATAR_URL));
            user.changeGender(Gender.fromCode(1));
            user.changeBirthday(Birthday.of(1995, 6, 15));

            User loaded = addAndReload(user);

            assertThat(loaded.getAvatar().getUrl()).isEqualTo(AVATAR_URL);
            assertThat(loaded.getGender().getCode()).isEqualTo(1);
            assertThat(loaded.getBirthday().getYear()).isEqualTo(1995);
            assertThat(loaded.getBirthday().getMonth()).isEqualTo(6);
            assertThat(loaded.getBirthday().getDay()).isEqualTo(15);
        }

        @Test
        @DisplayName("生日没有年份：年份存为 NULL，月日正常")
        void birthdayWithoutYear() {
            User user = newUser(nextId());
            user.changeBirthday(Birthday.of(null, 6, 15));

            User loaded = addAndReload(user);

            assertThat(loaded.getBirthday()).isNotNull();
            assertThat(loaded.getBirthday().getYear()).isNull();
            assertThat(loaded.getBirthday().getMonth()).isEqualTo(6);
            assertThat(loaded.getBirthday().getDay()).isEqualTo(15);
        }

        @Test
        @DisplayName("用户不存在时 findById 返回 null")
        void findByIdMissing() {
            assertThat(userRepository.findById(nextId())).isNull();
        }

        @Test
        @DisplayName("主键重复时插入失败")
        void duplicateIdFails() {
            ShardedId id = nextId();
            userRepository.add(newUser(id));

            assertThatThrownBy(() -> userRepository.add(newUser(id)))
                    .isInstanceOf(DuplicateKeyException.class);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("禁用后状态落库，createdAt 不变")
        void banPersists() {
            User loaded = addAndReload(newUser(nextId()));
            Instant createdAt = loaded.getCreatedAt();

            loaded.ban();
            userRepository.update(loaded);
            User reloaded = userRepository.findById(loaded.getId());

            assertThat(reloaded.getStatus()).isEqualTo(UserStatus.DISABLED);
            assertThat(reloaded.getCreatedAt()).isEqualTo(createdAt);
            assertThat(reloaded.getUpdatedAt()).isAfterOrEqualTo(createdAt);
        }

        @Test
        @DisplayName("修改昵称、头像、性别、生日后落库")
        void profileChangesPersist() {
            User loaded = addAndReload(newUser(nextId()));

            loaded.changeDisplayName(name("Jerry"));
            loaded.changeAvatar(Avatar.from(AVATAR_URL));
            loaded.changeGender(Gender.fromCode(2));
            loaded.changeBirthday(Birthday.of(2000, 1, 2));
            userRepository.update(loaded);
            User reloaded = userRepository.findById(loaded.getId());

            assertThat(reloaded.getDisplayName().value()).isEqualTo("Jerry");
            assertThat(reloaded.getAvatar().getUrl()).isEqualTo(AVATAR_URL);
            assertThat(reloaded.getGender().getCode()).isEqualTo(2);
            assertThat(reloaded.getBirthday().getYear()).isEqualTo(2000);
        }

        @Test
        @DisplayName("清除头像和生日后，数据库里确实变成 NULL（updateStrategy = ALWAYS 的关键验证）")
        void clearingOptionalFieldsReachesDatabase() {
            User user = newUser(nextId());
            user.changeAvatar(Avatar.from(AVATAR_URL));
            user.changeBirthday(Birthday.of(1995, 6, 15));
            User loaded = addAndReload(user);
            assertThat(loaded.getAvatar()).isNotNull();
            assertThat(loaded.getBirthday()).isNotNull();

            loaded.changeAvatar(null);
            loaded.changeBirthday(null);
            userRepository.update(loaded);
            User reloaded = userRepository.findById(loaded.getId());

            assertThat(reloaded.getAvatar()).isNull();
            assertThat(reloaded.getBirthday()).isNull();
        }

        @Test
        @DisplayName("更新不存在的用户：不报错、也不会插入新行（现状；若改为抛异常需同步修改）")
        void updateMissingUserIsSilent() {
            User ghost = newUser(nextId());

            userRepository.update(ghost);

            assertThat(userRepository.findById(ghost.getId())).isNull();
        }
    }

    @Nested
    @DisplayName("注销")
    class Deregister {

        @Test
        @DisplayName("注销落库：状态、注销时间保存，个人信息在数据库中被清除")
        void deregisterPersistsTombstone() {
            User user = newUser(nextId());
            user.changeAvatar(Avatar.from(AVATAR_URL));
            user.changeGender(Gender.fromCode(1));
            user.changeBirthday(Birthday.of(1995, 6, 15));
            User loaded = addAndReload(user);

            loaded.deregister(DEREGISTERED_AT);
            userRepository.update(loaded);
            User tombstone = userRepository.findById(loaded.getId());

            assertThat(tombstone.getStatus()).isEqualTo(UserStatus.DELETED);
            assertThat(tombstone.isDeregistered()).isTrue();
            assertThat(tombstone.getDeregisteredAt()).isEqualTo(DEREGISTERED_AT);
            assertThat(tombstone.getDisplayName()).isEqualTo(com.ovelin.mall.user.domain.model.valueobject.DisplayName.deregistered());
            assertThat(tombstone.getAvatar()).isNull();
            assertThat(tombstone.getBirthday()).isNull();
            assertThat(tombstone.getGender()).isEqualTo(Gender.UnKnown);
            // 不可变的来源信息和主键保留
            assertThat(tombstone.getId().value()).isEqualTo(loaded.getId().value());
            assertThat(tombstone.getRegisterSource().code()).isEqualTo(REGISTER_SOURCE);
        }

        @Test
        @DisplayName("已注销用户仍能按 id 查到（订单等业务要展示“已注销用户”）")
        void tombstoneIsStillLoadable() {
            User loaded = addAndReload(newUser(nextId()));
            loaded.deregister(DEREGISTERED_AT);
            userRepository.update(loaded);

            assertThat(userRepository.findById(loaded.getId())).isNotNull();
        }

        @Test
        @DisplayName("从数据库读出的已注销用户，不能再注销、不能修改资料")
        void loadedTombstoneIsImmutable() {
            User loaded = addAndReload(newUser(nextId()));
            loaded.deregister(DEREGISTERED_AT);
            userRepository.update(loaded);

            User tombstone = userRepository.findById(loaded.getId());

            assertThatThrownBy(() -> tombstone.deregister(DEREGISTERED_AT))
                    .isInstanceOf(InvalidUserStatusException.class);
            assertThatThrownBy(() -> tombstone.changeDisplayName(name("Hacker")))
                    .isInstanceOf(InvalidUserStatusException.class);
        }

        @Test
        @DisplayName("注销时间按毫秒精度原样读回（同时验证时区配置一致）")
        void deregisteredAtRoundTripsExactly() {
            User loaded = addAndReload(newUser(nextId()));

            loaded.deregister(DEREGISTERED_AT);
            userRepository.update(loaded);

            assertThat(userRepository.findById(loaded.getId()).getDeregisteredAt())
                    .isEqualTo(DEREGISTERED_AT);
        }
    }
}