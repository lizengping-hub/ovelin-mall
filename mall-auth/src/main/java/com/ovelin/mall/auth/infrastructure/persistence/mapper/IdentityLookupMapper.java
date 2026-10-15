package com.ovelin.mall.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ovelin.mall.auth.infrastructure.persistence.po.IdentityLookupPO;
import org.apache.ibatis.annotations.*;

/** 联合主键表，不继承 BaseMapper，全部手写 SQL。 */
@Mapper
public interface IdentityLookupMapper extends BaseMapper<IdentityLookupPO> {

    default IdentityLookupPO selectByTypeAndIdentifier(String identityType,
                                               String identifier){
        return selectOne(Wrappers.<IdentityLookupPO>lambdaQuery()
                .eq(IdentityLookupPO::getIdentityType, identityType)
                .eq(IdentityLookupPO::getIdentifier, identifier));
    }


    @Delete("""
            DELETE FROM identity_lookup
            WHERE identity_type = #{identityType} AND identifier = #{identifier}
            """)
    default int deleteByTypeAndIdentifier(String identityType,
                                  String identifier){
        return delete(Wrappers.<IdentityLookupPO>lambdaQuery()
                .eq(IdentityLookupPO::getIdentityType, identityType)
                .eq(IdentityLookupPO::getIdentifier, identifier));
    }
}