package com.ovelin.mall.user.infrastructure.persistence.repository;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
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
        UserPO userPO = UserPOConverter.toPO(user);
        userMapper.updateById(userPO);
    }
}
