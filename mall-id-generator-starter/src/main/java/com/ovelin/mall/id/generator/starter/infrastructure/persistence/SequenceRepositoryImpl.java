package com.ovelin.mall.id.generator.starter.infrastructure.persistence;

import com.ovelin.mall.id.generator.starter.IdGeneratorConstant;
import com.ovelin.mall.id.generator.starter.domain.module.ov.AllocationSize;
import com.ovelin.mall.id.generator.starter.domain.module.ov.Segment;
import com.ovelin.mall.id.generator.starter.domain.module.ov.SequenceName;
import com.ovelin.mall.id.generator.starter.domain.port.SequenceRepository;
import com.ovelin.mall.sharding.starter.api.ShardedJdbcExecutor;

public class SequenceRepositoryImpl implements SequenceRepository {
    private final ShardedJdbcExecutor executor;

    public SequenceRepositoryImpl(ShardedJdbcExecutor executor) {
        this.executor = executor;
    }

    @Override
    public Segment nextSegment(SequenceName name) {
        return executor.executeInTransaction(IdGeneratorConstant.SHARD_GROUP_KEY, (jdbcTemplate, route) -> {
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
        executor.executeInTransaction(IdGeneratorConstant.SHARD_GROUP_KEY, (jdbcTemplate, route) -> {
            String tableName = route.resolveTableName(IdGeneratorConstant.ID_SEQUENCE_TABLE_NAME);
//            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS " + tableName + """
//                    (
//                        sequence_name VARCHAR(128) NOT NULL,
//                        next_value BIGINT UNSIGNED NOT NULL DEFAULT 0,
//                        allocation_size INT UNSIGNED NOT NULL DEFAULT 1000,
//                        version BIGINT UNSIGNED NOT NULL DEFAULT 0,
//                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
//                            ON UPDATE CURRENT_TIMESTAMP,
//                        PRIMARY KEY (sequence_name)
//                    ) ENGINE=InnoDB
//                    DEFAULT CHARSET=utf8mb4
//                    COLLATE=utf8mb4_0900_ai_ci
//                    """);

            jdbcTemplate.update("""
                    INSERT INTO %s (sequence_name, next_value, allocation_size)
                    VALUES (?, 0, ?)
                    ON DUPLICATE KEY UPDATE sequence_name = sequence_name
                    """.formatted(tableName), name.value(), size.value());
            return null;
        });
    }
}
