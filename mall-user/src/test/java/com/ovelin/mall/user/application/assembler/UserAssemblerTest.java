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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

public class UserAssemblerTest {


    private final ShardedId id = ShardedId.of(100, 1000);
    private final com.ovelin.mall.user.domain.model.valueobject.DisplayName name = com.ovelin.mall.user.domain.model.valueobject.DisplayName.from("Tom");
    private final RegisterSource source = RegisterSource.UNKNOWN;

    /** 用 reconstitute 构造任意状态的用户，不依赖 setter。 */
    private User createUser() {
        return User.reconstitute(
                id, name, source, UserStatus.ACTIVE,
               null, null, null, 0L,
                Instant.now().truncatedTo(ChronoUnit.MILLIS),
                Instant.now().truncatedTo(ChronoUnit.MILLIS),
                null);
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
            User user = createUser();
            UserDTO userDTO = UserAssembler.toDTO(user);
            assertThat(userDTO).isNotNull();
            assertThat(userDTO.getId()).isEqualTo(id.value());
            assertThat(userDTO.getDisplayName()).isEqualTo(name.getValue());
            assertThat(userDTO.getRegisterSource()).isEqualTo(source.code());
            assertThat(userDTO.getStatus()).isEqualTo(UserStatus.ACTIVE.getValue());
            assertThat(userDTO.getGender()).isEqualTo(Gender.UnKnown.getCode());
            assertThat(userDTO.getAvatarUrl()).isNull();
            assertThat(userDTO.getBirthYear()).isNull();
            assertThat(userDTO.getBirthMonth()).isNull();
            assertThat(userDTO.getBirthDay()).isNull();
            assertThat(userDTO.getCreatedAt()).isEqualTo(user.getCreatedAt());
            assertThat(userDTO.getUpdatedAt()).isEqualTo(user.getUpdatedAt());
            assertThat(userDTO.getDeregisteredAt()).isNull();
        }

        @Test
        void shouldAssembleNotNullValue(){

            User user = createUser();
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
            User user = createUser();
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
                    ()->UserAssembler.toUser(null))
                    .isInstanceOf( NullPointerException.class)
                    .hasMessage("CreateUserCommand cannot be null");
        }
        @Test
        void shouldAssembleNullValue(){
            ShardedId shardedId = ShardedId.of(1, 1);
            CreateUserCommand command = CreateUserCommand.builder(shardedId, "Tom", RegisterSource.IOS.code()).build();
            User user = UserAssembler.toUser(command);
            assertThat(user).isNotNull();
            assertThat(user.getId()).isEqualTo(shardedId);
            assertThat(user.getDisplayName().getValue()).isEqualTo("Tom");
            assertThat(user.getRegisterSource().code()).isEqualTo(RegisterSource.IOS.code());
            assertThat(user.getGender().getCode()).isEqualTo(Gender.UnKnown.getCode());

            assertThat(user.getAvatar()).isNull();
            assertThat(user.getBirthday()).isNull();
            assertThat(user.getDeregisteredAt()).isNull();
            assertThat(user.getUpdatedAt()).isNull();
            assertThat(user.getCreatedAt()).isNull();
        }

        @Test
        void shouldAssembleNotNullValue(){

            ShardedId shardedId = ShardedId.of(1, 1);
            CreateUserCommand command = CreateUserCommand
                    .builder(shardedId, "Tom", RegisterSource.IOS.code())
                    .avatarUrl(Avatar.defaultAvatar().getUrl())
                    .birthYear(2000)
                    .birthDay(12, 12)
                    .gender(Gender.Female.getCode())
                    .build();
            User user = UserAssembler.toUser(command);
            assertThat(user).isNotNull();
            assertThat(user.getId()).isEqualTo(shardedId);
            assertThat(user.getDisplayName().getValue()).isEqualTo("Tom");
            assertThat(user.getRegisterSource().code()).isEqualTo(RegisterSource.IOS.code());
            assertThat(user.getGender().getCode()).isEqualTo(Gender.Female.getCode());

            assertThat(user.getAvatar()).isEqualTo(Avatar.defaultAvatar());
            assertThat(user.getBirthday()).isEqualTo(Birthday.of(2000, 12, 12));

        }
    }
}
