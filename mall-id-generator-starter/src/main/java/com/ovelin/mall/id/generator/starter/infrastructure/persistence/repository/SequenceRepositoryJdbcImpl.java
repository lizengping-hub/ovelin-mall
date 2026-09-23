package com.ovelin.mall.id.generator.starter.infrastructure.persistence.repository;

import com.ovelin.mall.id.generator.starter.domain.module.valueobject.AllocationSize;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.Segment;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.SequenceName;
import com.ovelin.mall.id.generator.starter.domain.port.SequenceRepository;
import com.ovelin.mall.id.generator.starter.infrastructure.persistence.IdGeneratorConstant;
import com.ovelin.mall.sharding.starter.api.ShardedJdbcExecutor;

public class SequenceRepositoryJdbcImpl implements SequenceRepository {
    private final ShardedJdbcExecutor executor;

    public SequenceRepositoryJdbcImpl(ShardedJdbcExecutor shardedJdbcExecutor) {
        this.executor = shardedJdbcExecutor;
    }

    @Override
    public Segment nextSegment(SequenceName name) {
        return executor.executeInTransaction(IdGeneratorConstant.SHARED_GROUP_KEY, (jdbcTemplate, route) -> {
            String tableName = route.resolveTableName(IdGeneratorConstant.ID_SEQUENCE_TABLE_NAME);

            Segment segment = jdbcTemplate.query("""
                    SELECT next_value, allocation_size
                    FROM %s
                    WHERE sequence_name = ? for update
                    """.formatted(tableName), rs -> {
                if (rs.next()) {
                    long nextValue = rs.getLong("next_value");
                    int allocationSize = rs.getInt("allocation_size");
                    return  Segment.fromStartAndSize(nextValue, allocationSize);
                } else {
                    throw new IllegalStateException("Sequence not found: " + name.value());
                }
            }, name.value());

            jdbcTemplate.update("""
                    UPDATE %s
                    SET next_value = next_value + allocation_size,
                        version = version + 1
                    WHERE sequence_name = ?
                    """.formatted(tableName), name.value());
            return segment;
        });
    }

    @Override
    public void initIfAbsent(SequenceName name, AllocationSize size) {
        executor.executeInTransaction(IdGeneratorConstant.SHARED_GROUP_KEY, (jdbcTemplate, route) -> {
            String tableName = route.resolveTableName(IdGeneratorConstant.ID_SEQUENCE_TABLE_NAME);
            jdbcTemplate.update("""
                    INSERT INTO %s (sequence_name, next_value, allocation_size)
                    VALUES (?, 0, ?)
                    ON DUPLICATE KEY UPDATE sequence_name = sequence_name
                    """.formatted(tableName), name.value(), size.value());
            return null;
        });
    }
}
