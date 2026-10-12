package com.ovelin.mall.user.application.service;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.user.api.UserDTO;
import com.ovelin.mall.user.api.UserQueryService;
import com.ovelin.mall.user.infrastructure.persistence.mapper.UserQueryMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserQueryApplicationService implements UserQueryService {
    private final UserQueryMapper userQueryMapper;
    @Override
    public UserDTO findById(Long id) {
        return userQueryMapper.selectById(ShardedId.fromId(id).value());
    }
}
