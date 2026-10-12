package com.ovelin.mall.user.application.api;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.id.generator.api.IdGenerationCommand;
import com.ovelin.mall.id.generator.api.IdGenerator;
import com.ovelin.mall.user.api.CreateUserCommand;
import com.ovelin.mall.user.api.UserApi;
import com.ovelin.mall.user.application.assembler.UserAssembler;
import com.ovelin.mall.user.domain.UserIdGenerationCommand;
import com.ovelin.mall.user.domain.model.aggregate.User;
import com.ovelin.mall.user.domain.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserApiImpl implements UserApi {
    UserRepository userRepository;
    IdGenerator idGenerator;


    @Override
    public ShardedId createUser(CreateUserCommand command) {
        IdGenerationCommand idGenerationCommand = UserIdGenerationCommand.builder() .shardId(command.getShardId()).build();
        ShardedId id = idGenerator.nextId(idGenerationCommand);
        User user = UserAssembler.toUser(command, id);
        userRepository.add(user);
        return user.getId();
    }

}
