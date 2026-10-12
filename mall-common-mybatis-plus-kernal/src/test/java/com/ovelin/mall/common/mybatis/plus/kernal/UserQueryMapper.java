package com.ovelin.mall.common.mybatis.plus.kernal;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserReadOnlyMapper extends ReadOnlyMapper<UserPO>{
}
