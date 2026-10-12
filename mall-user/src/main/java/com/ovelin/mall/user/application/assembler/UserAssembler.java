package com.ovelin.mall.user.application.assembler;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.user.api.CreateUserCommand;
import com.ovelin.mall.user.api.UserDTO;
import com.ovelin.mall.user.domain.model.aggregate.User;
import com.ovelin.mall.user.domain.model.enums.Gender;
import com.ovelin.mall.user.domain.model.enums.RegisterSource;
import com.ovelin.mall.user.domain.model.valueobject.*;

import java.util.Objects;
import java.util.Optional;

public class UserAssembler {
    public static UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId().value());
        userDTO.setDisplayName(user.getDisplayName()==null?null:user.getDisplayName().getValue());
        userDTO.setAvatarUrl(user.getAvatar()==null?null:user.getAvatar().getUrl());
        userDTO.setGender(user.getGender()==null?null:user.getGender().getCode());
        if (user.getBirthday() != null) {
            userDTO.setBirthYear(user.getBirthday().getYear());
            userDTO.setBirthMonth(user.getBirthday().getMonth());
            userDTO.setBirthDay(user.getBirthday().getDay());
        }
        userDTO.setStatus(user.getStatus().getValue());
        userDTO.setRegisterSource(user.getRegisterSource().code());
        userDTO.setDeregisteredAt(user.getDeregisteredAt());
        userDTO.setCreatedAt(user.getCreatedAt());
        userDTO.setUpdatedAt(user.getUpdatedAt());
        return userDTO;
    }

    /** Command -> 新用户。手机/邮箱由认证模块处理，这里只管资料。 */
    public static User toUser(CreateUserCommand cmd, ShardedId id) {
        Objects.requireNonNull(cmd, "CreateUserCommand cannot be null");
        Optional<Birthday> birthdayOptional = Birthday.ofNullable(cmd.getBirthYear(), cmd.getBirthMonth(), cmd.getBirthDay());
        User user = User.register(id,
                DisplayName.from(cmd.getDisplayName()),
                RegisterSource.fromCode(cmd.getRegisterSource()));

        if (cmd.getAvatarUrl() != null)
            user.changeAvatar(Avatar.from(cmd.getAvatarUrl()));
        if (cmd.getGender() != null)
            user.changeGender(Gender.fromCode(cmd.getGender()));

        birthdayOptional.ifPresent(user::changeBirthday);

        return user;
    }

}
