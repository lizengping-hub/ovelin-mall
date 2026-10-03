package com.ovelin.mall.common.sharding.starter.integration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;


import com.ovelin.mall.common.sharding.starter.integration.po.ShardingTestUserPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShardingTestUserMapper extends BaseMapper<ShardingTestUserPO> {
}
