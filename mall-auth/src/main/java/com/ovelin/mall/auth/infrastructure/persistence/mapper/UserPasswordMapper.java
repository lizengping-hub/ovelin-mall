package com.ovelin.mall.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ovelin.mall.auth.infrastructure.persistence.po.UserPasswordPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserPasswordMapper extends BaseMapper<UserPasswordPO> {
    // selectById / insert / updateById / deleteById 都按 user_id 路由，够用
}