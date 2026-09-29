package com.ovelin.mall.id.generator.starter.infrastructure.persistence.mapper;

import com.ovelin.mall.id.generator.starter.infrastructure.persistence.po.IdSequencePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface IdSequenceMapper {

    @Insert("""
            INSERT INTO id_sequence (sequence_name, shard_id, next_value)
            VALUES (#{sequenceName}, #{shardId}, 0)
            ON DUPLICATE KEY UPDATE sequence_name = sequence_name
            """)
    int insertIfAbsent(
            @Param("sequenceName") String sequenceName,
            @Param("shardId") int shardId);

    @Select("""
            SELECT sequence_name, shard_id, next_value, version
            FROM id_sequence
            WHERE sequence_name = #{sequenceName} and shard_id = #{shardId}
            FOR UPDATE
            """)
    IdSequencePO selectByName(
            @Param("sequenceName") String sequenceName,
            @Param("shardId") int shardId);

    @Update("""
            UPDATE id_sequence
            SET next_value = next_value + #{allocationSize},
                version = version + 1
            WHERE sequence_name = #{sequenceName} and shard_id = #{shardId}
            """)
    int advance(
            @Param("sequenceName") String sequenceName,
            @Param("shardId") int shardId,
            @Param("allocationSize") int allocationSize);
}
