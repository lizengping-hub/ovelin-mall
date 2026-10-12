package com.ovelin.mall.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ovelin.mall.auth.infrastructure.persistence.po.UserIdentityPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserIdentityMapper extends BaseMapper<UserIdentityPO> {
}
