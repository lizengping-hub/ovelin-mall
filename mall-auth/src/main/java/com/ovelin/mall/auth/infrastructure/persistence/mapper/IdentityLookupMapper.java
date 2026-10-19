package com.ovelin.mall.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ovelin.mall.auth.domain.model.enums.IdentityLookupStatus;
import com.ovelin.mall.auth.domain.model.identityIdentifier.IdentityIdentifier;
import com.ovelin.mall.auth.infrastructure.persistence.po.IdentityLookupPO;
import org.apache.ibatis.annotations.*;

import java.time.Instant;
import java.util.List;


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

    @Insert("""
            INSERT INTO identity_lookup (identity_type, identifier, user_id, status)
            VALUES (#{identityType}, #{identifier}, #{userId}, #{status})
            ON DUPLICATE KEY UPDATE
                updated_at = IF(user_id = VALUES(user_id) AND status = VALUES(status), NOW(3), updated_at)
            """)
    int insertOrTouch(
            @Param("identityType") String identityType,
            @Param("identifier") String identifier,
            @Param("userId") Long userId,
            @Param("status") int status
            );

    @Update("""
            UPDATE identity_lookup
            SET status = #{to}, updated_at = NOW(3)
            WHERE identity_type = #{identityType} AND identifier = #{identifier}
              AND user_id = #{userId} AND status = #{from}
              AND updated_at < DATE_SUB(NOW(3), INTERVAL #{olderThanSeconds} SECOND)
            """)
    int transitionIfStale(
            @Param("identityType") String identityType,
            @Param("identifier") String identifier,
            @Param("userId") Long userId,
            @Param("from") int from,
            @Param("to") int to,
            @Param("olderThanSeconds") long olderThanSeconds
    );

    @Delete("""
            DELETE FROM identity_lookup
            WHERE identity_type = #{identityType} AND identifier = #{identifier}
              AND user_id = #{userId} AND status = #{status}
              AND updated_at < DATE_SUB(NOW(3), INTERVAL #{olderThanSeconds} SECOND)
            """)
    int deleteIfStale(
            @Param("identityType") String identityType,
            @Param("identifier") String identifier,
            @Param("userId") Long userId,
            @Param("status") int status,
            @Param("olderThanSeconds") long olderThanSeconds
    );

    @Update("""
            UPDATE identity_lookup SET updated_at = NOW(3)
            WHERE identity_type = #{identityType} AND identifier = #{identifier}
              AND user_id = #{userId} AND status = #{status}
            """)
    int touch(
            @Param("identityType") String identityType,
            @Param("identifier") String identifier,
            @Param("userId") Long userId,
            @Param("status") int status
    );

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
    @Select("""
            SELECT identity_type,identifier,user_id,status,updated_at,created_at
            FROM identity_lookup
            WHERE status = #{status}
                AND updated_at < DATE_SUB(NOW(3), INTERVAL #{olderThanSeconds} SECOND)
            ORDER BY updated_at
            LIMIT #{limit}
            """)
    List<IdentityLookupPO> selectStale(
            @Param("status") int status,
            @Param("olderThanSeconds") long olderThanSeconds,
            @Param("limit") int limit
    );

    /** 带分片键 (identity_type, identifier) 的条件 */
    private LambdaQueryWrapper<IdentityLookupPO> byIdentifier(String type, String identifier) {
        return Wrappers.<IdentityLookupPO>lambdaQuery()
                .eq(IdentityLookupPO::getIdentityType, type)
                .eq(IdentityLookupPO::getIdentifier, identifier);
    }
}