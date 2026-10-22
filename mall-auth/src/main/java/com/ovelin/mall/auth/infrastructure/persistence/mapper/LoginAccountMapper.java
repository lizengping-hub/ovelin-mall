package com.ovelin.mall.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.ovelin.mall.auth.infrastructure.persistence.po.LoginAccountPO;
import org.apache.ibatis.annotations.Mapper;

import java.io.Serializable;

@Mapper
public interface LoginAccountMapper extends BaseMapper<LoginAccountPO> {
    default LoginAccountPO selectByUserId(long userId){
        return this.selectById(userId);
    }
    default int deleteByUserId(long userId){
        return this.deleteById(userId);
    }
    default int updateByUserId(LoginAccountPO po){
        return this.updateById(po);
    }
}
