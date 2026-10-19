package com.ovelin.mall.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ovelin.mall.auth.domain.model.enums.IdentityLookupStatus;
import com.ovelin.mall.auth.infrastructure.persistence.po.IdentityLookupPO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * identity_lookup 的 Mapper。联合主键 (identity_type, identifier)，同时也是分片键。
 * 简单的单条件查询、状态流转用 Wrapper；需要原子校验多个条件、或要用数据库时间的操作用手写 SQL。
 * <p>
 * updated_at 用于回收任务判断"是否过期"，凡是会改变记录状态、或表示操作仍在进行的写入，都必须刷新它
 * （这里全部在 SQL 里显式写出，不依赖列上的 ON UPDATE CURRENT_TIMESTAMP）。
 */
@Mapper
public interface IdentityLookupMapper extends BaseMapper<IdentityLookupPO> {

    default IdentityLookupPO selectByIdentifier(String identityType,
                                                String identifier) {
        return selectOne(byIdentifier(identityType, identifier));
    }

    default IdentityLookupPO selectActive(String identityType,
                                          String identifier) {
        return selectOne(byIdentifier(identityType, identifier).eq(IdentityLookupPO::getStatus, IdentityLookupStatus.ACTIVE.getCode()));
    }

    default int deleteByIdentifier(String identityType,
                                   String identifier) {
        return delete(byIdentifier(identityType, identifier));
    }

    /**
     * 占位或重试：不存在则插入；已存在则只有"同一用户、同一状态"才刷新 updated_at，别人的记录原样不动。
     * 冲突时直接加 X 锁，不会出现"INSERT 冲突持有 S 锁 → 再 UPDATE 升级 X 锁"的并发死锁。
     * 返回的影响行数不能用来判断是否插入成功，调用方必须再读一次校验属主和状态。
     */
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

    /**
     * 回收任务用：选出过期记录。不带分片键，会广播到所有分片。
     * 按 updated_at 升序，保证最久没处理的先被选中。
     */
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
    /** 回收用：只修改仍处于 from 状态、属于该用户、且 updated_at 仍早于阈值的记录 */
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

    /** 回收用：只删除处于该状态、属于该用户、且 updated_at 仍早于阈值的记录 */
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

    /** 刷新 updated_at：只作用于属于该用户、且仍处于该状态的记录（如重复 beginRelease 表示释放仍在进行） */
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

    /** 状态流转：from → to，同时刷新 updated_at */
    default int transition(String type, String identifier, long userId, IdentityLookupStatus from, IdentityLookupStatus to) {
        return this.update(null, Wrappers.<IdentityLookupPO>lambdaUpdate()
                .set(IdentityLookupPO::getStatus, to.getCode())
                .setSql("updated_at = NOW(3)")
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


    /** 带分片键 (identity_type, identifier) 的条件 */
    private LambdaQueryWrapper<IdentityLookupPO> byIdentifier(String type, String identifier) {
        return Wrappers.<IdentityLookupPO>lambdaQuery()
                .eq(IdentityLookupPO::getIdentityType, type)
                .eq(IdentityLookupPO::getIdentifier, identifier);
    }
}