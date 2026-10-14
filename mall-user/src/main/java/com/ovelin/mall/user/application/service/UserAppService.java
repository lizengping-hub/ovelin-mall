package com.ovelin.mall.user.application.service;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.user.api.CreateUserCommand;
import com.ovelin.mall.user.api.UserApi;
import com.ovelin.mall.user.application.assembler.UserAssembler;
import com.ovelin.mall.user.domain.exception.UserNotFoundException;
import com.ovelin.mall.user.domain.model.aggregate.User;
import com.ovelin.mall.user.domain.model.enums.Gender;
import com.ovelin.mall.user.domain.model.valueobject.*;
import com.ovelin.mall.user.domain.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@AllArgsConstructor
public class UserAppService implements UserApi {
    UserRepository userRepository;

    public void updateDisplayName(Long id, String displayName) {
        DisplayName displayNameObj = DisplayName.from(displayName);
        User user = getUser(id);
        user.changeDisplayName(displayNameObj);
        userRepository.update(user);
    }


    public void updateBirthDate(Long id, Integer birthYear, Integer birthMonth, Integer birthDay) {
        Birthday birthday = Birthday.of(birthYear, birthMonth, birthDay);
        User user = getUser(id);
        user.changeBirthday(birthday);
        userRepository.update(user);
    }


    public void updateAvatarUrl(Long id, String avatarUrl) {
        Avatar avatar = Avatar.from(avatarUrl);
        User user = getUser(id);
        user.changeAvatar(avatar);
        userRepository.update(user);
    }


    public void updateGender(Long id, Integer gender) {
        Gender genderEnum = Gender.fromCode(gender);
        User user = getUser(id);
        user.changeGender(genderEnum);
        userRepository.update(user);
    }
    @Override
    public void deregister(Long id, Instant at) {
        User user = getUser(id);
        user.deregister(at);
        userRepository.update(user);
    }

    @Override
    public void createUser(CreateUserCommand command) {
        User user = UserAssembler.toUser(command);
        userRepository.add(user);
    }

    @Override
    public void createUser(Long userId, String displayName, String registerSource) {
        CreateUserCommand command = CreateUserCommand.builder(ShardedId.fromId(userId), displayName, registerSource).build();
        createUser(command);
    }

    private User getUser(Long id) {
        ShardedId shardedId = ShardedId.fromId(id);
        User user = userRepository.findById(shardedId);
        if (user == null) throw new UserNotFoundException(id);
        return user;
    }
}
