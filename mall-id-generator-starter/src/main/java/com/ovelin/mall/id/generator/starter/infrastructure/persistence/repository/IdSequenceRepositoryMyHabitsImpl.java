package com.ovelin.mall.id.generator.starter.infrastructure.persistence.repository;

import com.ovelin.mall.id.generator.starter.domain.module.valueobject.AllocationSize;
import com.ovelin.mall.id.generator.starter.domain.module.Segment;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.SequenceName;
import com.ovelin.mall.id.generator.starter.domain.repository.SequenceRepository;

import com.ovelin.mall.id.generator.starter.infrastructure.persistence.IdGeneratorConstant;
import com.ovelin.mall.id.generator.starter.infrastructure.persistence.mapper.IdSequenceMapper;
import com.ovelin.mall.id.generator.starter.infrastructure.persistence.po.IdSequencePO;
import com.ovelin.mall.sharding.starter.api.ShardedMyBatisExecutor;

public class IdSequenceRepositoryMyHabitsImpl implements SequenceRepository {

    private final ShardedMyBatisExecutor executor;
    public IdSequenceRepositoryMyHabitsImpl(ShardedMyBatisExecutor executor) {
        this.executor = executor;
    }

    @Override
    public Segment allocateSegment(SequenceName name) {

        return executor.executeInTransaction(IdGeneratorConstant.SHARED_GROUP_KEY, (myBatisClient, route) -> {
            String tableName = route.resolveTableName(IdGeneratorConstant.ID_SEQUENCE_TABLE_NAME);
            IdSequenceMapper mapper = myBatisClient.mapper(IdSequenceMapper.class);

            IdSequencePO sequence = mapper.selectByName(tableName, name.value());
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

        executor.execute(IdGeneratorConstant.SHARED_GROUP_KEY, (myBatisClient, resolvedRoute) -> {
            String tableName = resolvedRoute.resolveTableName(IdGeneratorConstant.ID_SEQUENCE_TABLE_NAME);

            IdSequenceMapper mapper = myBatisClient
                    .mapper(IdSequenceMapper.class);
            mapper.insertIfAbsent(tableName, name.value(), size.value());
            return null;
        });
    }
}
