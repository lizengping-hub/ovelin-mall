package com.ovelin.mall.user.application.assembler;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.user.api.CreateUserCommand;
import com.ovelin.mall.user.api.UserDTO;
import com.ovelin.mall.user.domain.model.aggregate.User;
import com.ovelin.mall.user.domain.model.enums.Gender;
import com.ovelin.mall.user.domain.model.enums.RegisterSource;
import com.ovelin.mall.user.domain.model.enums.UserStatus;
import com.ovelin.mall.user.domain.model.valueobject.Avatar;
import com.ovelin.mall.user.domain.model.valueobject.Birthday;
import com.ovelin.mall.user.domain.model.valueobject.UserSnapshot;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UserAssemblerTest {


    private final ShardedId id = ShardedId.of(100, 1000);
    private final com.ovelin.mall.user.domain.model.valueobject.DisplayName name = com.ovelin.mall.user.domain.model.valueobject.DisplayName.from("Tom");
    private final RegisterSource source = RegisterSource.UNKNOWN;

    /** 用 reconstitute 构造任意状态的用户，不依赖 setter。 */
    private User createWith() {
        return User.reconstitute(new UserSnapshot(
                id, name, source, UserStatus.ACTIVE,
               null, null, null, Instant.now().truncatedTo(ChronoUnit.MILLIS), Instant.now().truncatedTo(ChronoUnit.MILLIS), null));
    }
    @Nested
    class ToDTO {
        @Test
        void shouldAssembleNullUser(){
            UserDTO dto = UserAssembler.toDTO(null);
            assertThat(dto).isNull();;
        }

        @Test
        void shouldAssembleNullValue(){
            User user = createWith();
            UserDTO userDTO = UserAssembler.toDTO(user);
            assertThat(userDTO).isNotNull();
            assertThat(userDTO.getId()).isEqualTo(id.value());
            assertThat(userDTO.getDisplayName()).isEqualTo(name.getValue());
            assertThat(userDTO.getRegisterSource()).isEqualTo(source.code());
            assertThat(userDTO.getStatus()).isEqualTo(UserStatus.ACTIVE.getValue());
            assertThat(userDTO.getAvatarUrl()).isNull();
            assertThat(userDTO.getGender()).isEqualTo(Gender.UnKnown.getCode());
            assertThat(userDTO.getBirthYear()).isNull();
            assertThat(userDTO.getBirthMonth()).isNull();
            assertThat(userDTO.getBirthDay()).isNull();
            assertThat(userDTO.getCreatedAt()).isEqualTo(user.getCreatedAt());
            assertThat(userDTO.getUpdatedAt()).isEqualTo(user.getUpdatedAt());
            assertThat(userDTO.getDeregisteredAt()).isNull();
        }

        @Test
        void shouldAssembleNotNullValue(){

            User user = createWith();
            user.changeAvatar(Avatar.defaultAvatar());
            user.changeBirthday(Birthday.of(2000, 1,1));
            user.changeGender(Gender.Male);
            UserDTO userDTO = UserAssembler.toDTO(user);
            assertThat(userDTO.getAvatarUrl()).isEqualTo(user.getAvatar().getUrl());
            assertThat(userDTO.getGender()).isEqualTo(user.getGender().getCode());
            assertThat(userDTO.getBirthYear()).isEqualTo(user.getBirthday().getYear());
            assertThat(userDTO.getBirthMonth()).isEqualTo(user.getBirthday().getDay());
            assertThat(userDTO.getBirthDay()).isEqualTo(user.getBirthday().getDay());
        }
        @Test
        void shouldAssembleNullBirthYearValue(){
            User user = createWith();
            user.changeBirthday(Birthday.of(null, 1,1));

            UserDTO userDTO = UserAssembler.toDTO(user);
            assertThat(userDTO.getBirthYear()).isNull();
            assertThat(userDTO.getBirthMonth()).isEqualTo(user.getBirthday().getDay());
            assertThat(userDTO.getBirthDay()).isEqualTo(user.getBirthday().getDay());
        }
    }
    @Nested
    class ToUser{
        @Test
        void shouldThrowsNullPointerExceptionWhileCommandIsNull(){
            assertThatThrownBy(
                    ()->UserAssembler.toUser(null, ShardedId.fromId(1)))
                    .isInstanceOf( NullPointerException.class)
                    .hasMessage("CreateUserCommand cannot be null");
        }
        @Test
        void shouldAssembleNullValue(){
            CreateUserCommand command = CreateUserCommand.builder("Tom", RegisterSource.IOS.code()).build();
            ShardedId shardedId = ShardedId.of(1, 100);
            User user = UserAssembler.toUser(command, shardedId);
        }
    }
}
