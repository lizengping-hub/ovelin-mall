package com.ovelin.mall.user.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ovelin.mall.user.infrastructure.persistence.po.UserPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserUpdateMapper extends BaseMapper<UserPO> {
}
