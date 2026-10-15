package com.ovelin.mall.user.infrastructure.persistence.repository;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.user.domain.exception.UserConcurrentUpdateException;
import com.ovelin.mall.user.domain.exception.UserNotFoundException;
import com.ovelin.mall.user.domain.model.aggregate.User;
import com.ovelin.mall.user.domain.repository.UserRepository;
import com.ovelin.mall.user.infrastructure.persistence.converter.UserPOConverter;
import com.ovelin.mall.user.infrastructure.persistence.mapper.UserUpdateMapper;
import com.ovelin.mall.user.infrastructure.persistence.po.UserPO;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class UserRepositoryImpl implements UserRepository{
    UserUpdateMapper userMapper;
    @Override
    public User findById(ShardedId id) {
        UserPO userPO = userMapper.selectById(id.value());
        return UserPOConverter.toUser(userPO);
    }

    @Override
    public void add(User user) {
        UserPO userPO = UserPOConverter.toPO(user);
        userMapper.insert(userPO);
    }
    @Override
    public void update(User user) {
        UserPO po = UserPOConverter.toPO(user);
        int rows = userMapper.updateById(po);
        if (rows == 0) {
            if (userMapper.selectById(po.getId()) == null) {
                throw new UserNotFoundException(user.getId().value());   // → 404
            }
            throw new UserConcurrentUpdateException(user);                   // → 409
        }
        user.syncVersion(po.getVersion());

    }
}
