package com.ovelin.mall.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ovelin.mall.auth.infrastructure.persistence.po.UserMfaFactorPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMfaFactorMapper extends BaseMapper<UserMfaFactorPO> {

    default List<UserMfaFactorPO> selectByUserId(Long userId) {
        return selectList(Wrappers.<UserMfaFactorPO>lambdaQuery()
                .eq(UserMfaFactorPO::getUserId, userId));
    }

    default int deleteByUserId(Long userId) {
        return delete(Wrappers.<UserMfaFactorPO>lambdaQuery()
                .eq(UserMfaFactorPO::getUserId, userId));
    }
}