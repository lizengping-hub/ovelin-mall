package com.ovelin.mall.common.sharding.starter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ovelin.mall.common.sharding.starter.po.IdentityPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserIdentityMapper extends BaseMapper<IdentityPO> {
}
