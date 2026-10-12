package com.ovelin.mall.user.application.it;

import com.ovelin.mall.common.sharding.core.ov.ShardId;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.user.api.CreateUserCommand;
import com.ovelin.mall.user.api.UserApi;
import com.ovelin.mall.user.api.UserDTO;
import com.ovelin.mall.user.api.UserQueryService;
import com.ovelin.mall.user.application.service.UserApplicationService;
import com.ovelin.mall.user.domain.model.enums.RegisterSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.within;

@SpringBootTest
public class UserLifecycleIT {
    @Autowired
    UserApi userApi;
    @Autowired
    UserQueryService userQueryService;
    @Autowired
    UserApplicationService userApplicationService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        jdbcTemplate.execute("delete from user");
        jdbcTemplate.execute("delete from id_sequence");
    }

    private ShardedId createUser(String name) {
        return userApi.createUser(
                CreateUserCommand.builder(name, RegisterSource.WEB.code())
                        .shardId(ShardId.of(10)).build());
    }
    @Nested
    @DisplayName("用户创建和查询 测试")
    class CreateAndQuery{
        @Test
        @DisplayName("创建后查询")
        void createAndQuery(){
            String displayName = "Tom";
            ShardedId shardedId = createUser(displayName);
            assertThat(shardedId.shardId()).isEqualTo(ShardId.of(10));

            UserDTO dto = userQueryService.findById(shardedId.value());
            assertThat(dto).isNotNull();
            assertThat(dto.getDisplayName()).isEqualTo(displayName);
            assertThat(dto.getRegisterSource()).isEqualTo(RegisterSource.WEB.code());
            assertThat(dto.getDeregisteredAt()).isNull();
            assertThat(dto.getCreatedAt()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
            assertThat(dto.getUpdatedAt()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("查询不存在的用户不报错")
        void queryUserNotExit(){
            long id = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE/2, Long.MAX_VALUE);
            UserDTO dto = userQueryService.findById(id);
            assertThat(dto).isNull();
        }
    }

    @Nested
    class UpdateAndQuery{
        @Test
        @DisplayName("修改昵称后读回的是新值")
        void updateDisplayNamePersisted() {
            ShardedId id = createUser("Tom");
            userApplicationService.updateDisplayName(id.value(), "Andy");
            assertThat(userQueryService.findById(id.value()).getDisplayName()).isEqualTo("Andy");
        }

        @Test
        void deregisterAndQuery(){
            ShardedId shardedId = createUser("Tom");
            Instant deregistedInstant = Instant.now().truncatedTo(ChronoUnit.MILLIS);
            userApplicationService.deregister(shardedId.value(), deregistedInstant);

            UserDTO dto = userQueryService.findById(shardedId.value());
            assertThat(dto).isNotNull();
            assertThat(dto.getDisplayName()).isEqualTo(com.ovelin.mall.user.domain.model.valueobject.DisplayName.deregistered().getValue());
            assertThat(dto.getDeregisteredAt()).isEqualTo(deregistedInstant);
        }
    }
}
