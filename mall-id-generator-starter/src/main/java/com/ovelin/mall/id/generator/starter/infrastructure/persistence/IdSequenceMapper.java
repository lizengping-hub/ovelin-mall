package com.ovelin.mall.id.generator.starter.infrastructure.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface IdSequenceMapper {

    @Insert("""
            INSERT INTO ${tableName} (sequence_name, next_value, allocation_size)
            VALUES (#{sequenceName}, 0, #{allocationSize})
            ON DUPLICATE KEY UPDATE sequence_name = sequence_name
            """)
    int insertIfAbsent(
            @Param("tableName") String tableName,
            @Param("sequenceName") String sequenceName,
            @Param("allocationSize") int allocationSize);

    @Select("""
            SELECT sequence_name, next_value, allocation_size, version
            FROM ${tableName}
            WHERE sequence_name = #{sequenceName}
            FOR UPDATE
            """)
    IdSequence selectByName(
            @Param("tableName") String tableName,
            @Param("sequenceName") String sequenceName);

    @Update("""
            UPDATE ${tableName}
            SET next_value = next_value + allocation_size,
                version = version + 1
            WHERE sequence_name = #{sequenceName}
            """)
    int advance(
            @Param("tableName") String tableName,
            @Param("sequenceName") String sequenceName);
}
