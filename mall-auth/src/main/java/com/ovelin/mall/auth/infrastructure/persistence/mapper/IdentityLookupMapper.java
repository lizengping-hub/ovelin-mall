package com.ovelin.mall.auth.infrastructure.persistence.mapper;

import com.ovelin.mall.auth.infrastructure.persistence.po.IdentityLookupPO;
import org.apache.ibatis.annotations.*;

/** 联合主键表，不继承 BaseMapper，全部手写 SQL。 */
@Mapper
public interface IdentityLookupMapper {

    @Select("""
            SELECT identity_type, identifier, user_id, created_at
            FROM identity_lookup
            WHERE identity_type = #{identityType} AND identifier = #{identifier}
            """)
    IdentityLookupPO selectByTypeAndIdentifier(@Param("identityType") String identityType,
                                               @Param("identifier") String identifier);

    /** 主键冲突会抛 DuplicateKeyException，表示该标识已被占用 */
    @Insert("""
            INSERT INTO identity_lookup (identity_type, identifier, user_id)
            VALUES (#{identityType}, #{identifier}, #{userId})
            """)
    int insert(IdentityLookupPO po);

    @Delete("""
            DELETE FROM identity_lookup
            WHERE identity_type = #{identityType} AND identifier = #{identifier}
            """)
    int deleteByTypeAndIdentifier(@Param("identityType") String identityType,
                                  @Param("identifier") String identifier);
}