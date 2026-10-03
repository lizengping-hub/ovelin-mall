package com.ovelin.mall.common.sharding.starter.integration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.ovelin.mall.common.sharding.starter.integration.po.ShardingTestIdentityPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ShardingTestIdentityMapper extends BaseMapper<ShardingTestIdentityPO> {
    @Select("SELECT id, user_id, identity_type, identifier FROM sharding_test_identity WHERE identifier = #{identifier}")
    ShardingTestIdentityPO selectByIdentifier(@Param("identifier") String identifier);
}
