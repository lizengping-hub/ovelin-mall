package com.ovelin.mall.user.application.api;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.user.api.CreateUserCommand;
import com.ovelin.mall.user.api.UserApi;
import com.ovelin.mall.user.application.assembler.UserAssembler;
import com.ovelin.mall.user.domain.model.aggregate.User;
import com.ovelin.mall.user.domain.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserApiImpl implements UserApi {
    UserRepository userRepository;

    @Override
    public ShardedId createUser(CreateUserCommand command) {
        User user = UserAssembler.toUser(command);
        userRepository.add(user);
        return user.getId();
    }

}
