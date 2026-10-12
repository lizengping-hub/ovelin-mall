package com.ovelin.mall.common.mybatis.plus.kernal.baseQueryMapper;

import com.ovelin.mall.common.mybatis.plus.kernal.BaseQueryMapper;
import com.ovelin.mall.common.mybatis.plus.kernal.UserPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserQueryMapper extends BaseQueryMapper<UserPO> {
}
