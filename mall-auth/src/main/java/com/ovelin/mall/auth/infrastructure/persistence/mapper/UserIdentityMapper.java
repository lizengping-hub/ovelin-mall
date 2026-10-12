package com.ovelin.mall.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ovelin.mall.auth.infrastructure.persistence.po.UserIdentityPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserIdentityMapper extends BaseMapper<UserIdentityPO> {

    default List<UserIdentityPO> selectByUserId(Long userId) {
        return selectList(Wrappers.<UserIdentityPO>lambdaQuery()
                .eq(UserIdentityPO::getUserId, userId));
    }

    default int deleteByUserId(Long userId) {
        return delete(Wrappers.<UserIdentityPO>lambdaQuery()
                .eq(UserIdentityPO::getUserId, userId));
    }
}