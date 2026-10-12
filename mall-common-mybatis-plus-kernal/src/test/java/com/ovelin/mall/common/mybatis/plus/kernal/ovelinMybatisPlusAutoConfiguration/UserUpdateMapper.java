package com.ovelin.mall.common.mybatis.plus.kernal.ovelinMybatisPlusAutoConfiguration;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ovelin.mall.common.mybatis.plus.kernal.UserPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserUpdateMapper extends BaseMapper<UserPO> {
}
