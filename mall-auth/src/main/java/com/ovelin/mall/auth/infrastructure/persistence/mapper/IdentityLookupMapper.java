package com.ovelin.mall.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ovelin.mall.auth.domain.model.enums.IdentityLookupStatus;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifier;
import com.ovelin.mall.auth.infrastructure.persistence.po.IdentityLookupPO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/** 联合主键表，不继承 BaseMapper，全部手写 SQL。 */
@Mapper
public interface IdentityLookupMapper extends BaseMapper<IdentityLookupPO> {

    default IdentityLookupPO selectByIdentifier(String identityType,
                                                String identifier){
        return selectOne(byIdentifier(identityType, identifier));
    }
    default IdentityLookupPO selectActive(String identityType,
                                          String identifier){
        return selectOne(byIdentifier(identityType, identifier).eq(IdentityLookupPO::getStatus, IdentityLookupStatus.ACTIVE.getCode()));
    }

    default int deleteByIdentifier(String identityType,
                                   String identifier){
        return delete(byIdentifier(identityType, identifier));
    }


    default int transition(String type, String identifier, long userId, IdentityLookupStatus from, IdentityLookupStatus to) {
        return this.update(null, Wrappers.<IdentityLookupPO>lambdaUpdate()
                .set(IdentityLookupPO::getStatus, to.getCode())
                .eq(IdentityLookupPO::getIdentityType, type)
                .eq(IdentityLookupPO::getIdentifier, identifier)
                .eq(IdentityLookupPO::getUserId, userId)
                .eq(IdentityLookupPO::getStatus, from.getCode()));
    }

    default int deleteIfMatch(String type, String identifier, long userId, IdentityLookupStatus status) {
        return this.delete(byIdentifier(type, identifier)
                .eq(IdentityLookupPO::getUserId, userId)
                .eq(IdentityLookupPO::getStatus, status.getCode()));
    }

    default List<IdentityLookupPO> selectStale(IdentityLookupStatus status, long olderThan, int limit) {
        return this.selectList(Wrappers.<IdentityLookupPO>lambdaQuery()
                .eq(IdentityLookupPO::getStatus, status.getCode())
                .apply("updated_at < DATE_SUB(NOW(3), INTERVAL {0} SECOND)", olderThan)
                .last("LIMIT " + Math.max(1, limit)));
    }

    /** 带分片键 (identity_type, identifier) 的条件 */
    private LambdaQueryWrapper<IdentityLookupPO> byIdentifier(String type, String identifier) {
        return Wrappers.<IdentityLookupPO>lambdaQuery()
                .eq(IdentityLookupPO::getIdentityType, type)
                .eq(IdentityLookupPO::getIdentifier, identifier);
    }
}