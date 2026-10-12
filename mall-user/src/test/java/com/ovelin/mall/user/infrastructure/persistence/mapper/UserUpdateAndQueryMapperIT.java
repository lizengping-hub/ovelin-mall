package com.ovelin.mall.user.infrastructure.persistence.mapper;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.user.TestApplication;
import com.ovelin.mall.user.api.UserDTO;
import com.ovelin.mall.user.domain.model.aggregate.User;
import com.ovelin.mall.user.domain.model.enums.UserStatus;
import com.ovelin.mall.user.infrastructure.persistence.po.UserPO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.within;

@SpringBootTest(classes = TestApplication.class)
@EnableAutoConfiguration()
public class UserUpdateAndQueryMapperIT {
    @Autowired
    UserUpdateMapper userUpdateMapper;
    @Autowired
    UserQueryMapper userQueryMapper;
    @Autowired
    JdbcTemplate jdbcTemplate;

    /** 每个用例从空表开始,保证"状态为空的 repository"这个契约前提成立。 */
    @BeforeEach
    void cleanTable() {
        jdbcTemplate.execute("TRUNCATE TABLE user");
    }


    private UserPO newUser(long id, String displayName) {
        UserPO u = new UserPO();
        u.setId(id);
        u.setDisplayName(displayName);
        u.setStatus(UserStatus.ACTIVE.getValue());
        u.setRegisterSource("test");
        return u;   // 不要 set createdAt/updatedAt，它们由数据库生成
    }
    @Test
    void insert_thenQuery_roundTripsFields() {
        long id = ShardedId.of(1, 0L).value();
        UserPO user = newUser(id, "alice");
        user.setAvatarUrl("http://a/1.png");
        user.setBirthYear(1993);
        user.setBirthMonth(12);
        user.setBirthDay(12);
        userUpdateMapper.insert(user);

        UserDTO found = userQueryMapper.selectById(id);
        assertThat(found.getDisplayName()).isEqualTo("alice");
        assertThat(found.getAvatarUrl()).isEqualTo("http://a/1.png");
        assertThat(found.getRegisterSource()).isEqualTo("test");
        assertThat(found.getBirthYear()).isEqualTo(1993);
        assertThat(found.getBirthMonth()).isEqualTo(12);
        assertThat(found.getBirthDay()).isEqualTo(12);

        assertThat(found.getCreatedAt()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(found.getDeregisteredAt()).isNull();
    }
    @Test
    void insert_thenSelect_roundTripsFields() {
        long id = ShardedId.of(1, 1L).value();

        userUpdateMapper.insert(newUser(id, "alice"));

        UserPO found = userUpdateMapper.selectById(id);

        assertThat(found.getDisplayName()).isEqualTo("alice");
        assertThat(found.getBirthYear()).isNull();
        assertThat(found.getCreatedAt()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(found.getDeregisteredAt()).isNull();
    }

    @Test
    void update_updatesStatusAndDeregisteredAt() {
        long id = ShardedId.of(1, 2L).value();
        Instant deregisterAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        UserPO userPO = newUser(id, "bob");
        userUpdateMapper.insert(userPO);
        userPO.setDeregisteredAt(deregisterAt);
        userPO.setStatus(UserStatus.DELETED.getValue());
        userUpdateMapper.updateById(userPO);

        UserPO deletedUser =  userUpdateMapper.selectById(id);
        assertThat(deletedUser).isNotNull();
        assertThat(deletedUser.getStatus()).isEqualTo(UserStatus.DELETED.getValue());
        assertThat(deletedUser.getDeregisteredAt()).isEqualTo(deregisterAt);
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user WHERE id = ? and status = %d".formatted(UserStatus.DELETED.getValue()),
                Integer.class, id);
        assertThat(rows).isEqualTo(1);
    }


    @Test
    void sameUserId_rejectedWhenNotDeleted() {
        userUpdateMapper.insert(newUser(ShardedId.of(1, 5L).value(), "dave"));

        assertThatThrownBy(() -> userUpdateMapper.insert(newUser(ShardedId.of(1, 5L).value(), "dave")))
                .isInstanceOf(DuplicateKeyException.class);
    }
    @Test
    void update_shouldNotIgnoresNullFields() {
        long id = ShardedId.of(1, 7L).value();

        UserPO user = newUser(id, "erin");
        user.setAvatarUrl("http://a/1.png");
        user.setBirthYear(1993);
        user.setBirthMonth(12);
        user.setBirthDay(12);
        userUpdateMapper.insert(user);

        UserPO patch = new UserPO();
        patch.setId(id);

        userUpdateMapper.updateById(patch);

        UserPO after = userUpdateMapper.selectById(id);

        assertThat(after.getDisplayName()).isEqualTo("erin");
        assertThat(after.getAvatarUrl()).isNull();
        assertThat(after.getBirthYear()).isNull();
        assertThat(after.getBirthMonth()).isNull();
        assertThat(after.getBirthDay()).isNull();
    }
    @Test
    void update_bumpsUpdatedAt() throws Exception {
        long id = ShardedId.of(1, 8L).value();

        UserPO user = newUser(id, "erin");
        userUpdateMapper.insert(user);

        Instant before = userUpdateMapper.selectById(id).getUpdatedAt();

        Thread.sleep(1100);

        UserPO patch = new UserPO();
        patch.setId(id);
        patch.setDisplayName("lily");

        userUpdateMapper.updateById(patch);

        UserPO after = userUpdateMapper.selectById(id);

        assertThat(after.getUpdatedAt()).isAfter(before);
    }

}
