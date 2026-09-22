package com.ovelin.mall.id.generator.starter.infrastructure.persistence;

import com.ovelin.mall.id.generator.starter.IdGeneratorConstant;
import com.ovelin.mall.id.generator.starter.domain.module.ov.AllocationSize;
import com.ovelin.mall.id.generator.starter.domain.module.ov.Segment;
import com.ovelin.mall.id.generator.starter.domain.module.ov.SequenceName;
import com.ovelin.mall.id.generator.starter.domain.port.SequenceRepository;
import com.ovelin.mall.sharding.starter.api.ResolvedRoute;
import com.ovelin.mall.sharding.starter.api.ShardedJdbcExecutor;
import com.ovelin.mall.sharding.starter.core.DatabaseClientManager;

public class SequenceRepositoryMyHabitsImpl implements SequenceRepository {

    private final ShardedJdbcExecutor executor;
    private final DatabaseClientManager databaseClientManager;

    public SequenceRepositoryMyHabitsImpl(
            ShardedJdbcExecutor executor,
            DatabaseClientManager databaseClientManager) {
        this.executor = executor;
        this.databaseClientManager = databaseClientManager;
    }

    @Override
    public Segment nextSegment(SequenceName name) {
        ResolvedRoute route = executor.resolveRoute(IdGeneratorConstant.SHARD_GROUP_KEY);
        IdSequenceMapper mapper = databaseClientManager
                .getMyBatisClient(route.instanceKey())
                .mapper(IdSequenceMapper.class);
        String tableName = route.resolveTableName(IdGeneratorConstant.ID_SEQUENCE_TABLE_NAME);

        return executor.executeInTransaction(route, (jdbcTemplate, ignored) -> {
            IdSequence sequence = mapper.selectByName(tableName, name.value());
            if (sequence == null) {
                throw new IllegalStateException("Sequence not found: " + name.value());
            }

            Segment segment = Segment.fromStartAndSize(
                    sequence.nextValue(),
                    sequence.allocationSize());
            int updatedRows = mapper.advance(tableName, name.value());
            if (updatedRows != 1) {
                throw new IllegalStateException(
                        "Failed to advance sequence: " + name.value());
            }
            return segment;
        });
    }

    @Override
    public void initIfAbsent(SequenceName name, AllocationSize size) {
        ResolvedRoute route = executor.resolveRoute(IdGeneratorConstant.SHARD_GROUP_KEY);
        IdSequenceMapper mapper = databaseClientManager
                .getMyBatisClient(route.instanceKey())
                .mapper(IdSequenceMapper.class);
        String tableName = route.resolveTableName(IdGeneratorConstant.ID_SEQUENCE_TABLE_NAME);

        executor.executeInTransaction(route, (jdbcTemplate, ignored) -> {
            mapper.insertIfAbsent(tableName, name.value(), size.value());
            return null;
        });
    }
}
