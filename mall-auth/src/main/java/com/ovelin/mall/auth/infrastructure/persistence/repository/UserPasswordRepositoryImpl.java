package com.ovelin.mall.auth.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ovelin.mall.auth.domain.model.aggregate.UserPassword;
import com.ovelin.mall.auth.domain.repository.UserPasswordRepository;
import com.ovelin.mall.auth.infrastructure.persistence.convertor.UserPasswordPOConverter;
import com.ovelin.mall.auth.infrastructure.persistence.mapper.UserPasswordMapper;
import com.ovelin.mall.auth.infrastructure.persistence.po.UserPasswordPO;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserPasswordRepositoryImpl implements UserPasswordRepository {

    private final UserPasswordMapper mapper;

    @Override
    public void add(UserPassword password) {
        mapper.insert(UserPasswordPOConverter.toPO(password));
    }

    @Override
    public void update(UserPassword password) {
        mapper.updateById(UserPasswordPOConverter.toPO(password));
    }

    @Override
    public Optional<UserPassword> findByUserId(ShardedId userId) {
        return Optional.ofNullable(mapper.selectById(userId.value())).map(UserPasswordPOConverter::toDomain);
    }

    @Override
    public void deleteByUserId(ShardedId userId) {
        mapper.deleteById(userId.value());
    }
}