package com.ovelin.mall.user.infrastructure.persistence.mapper;

import com.ovelin.mall.common.mybatis.plus.kernal.BaseQueryMapper;
import com.ovelin.mall.user.api.UserDTO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserQueryMapper extends BaseQueryMapper<UserDTO> {
}
