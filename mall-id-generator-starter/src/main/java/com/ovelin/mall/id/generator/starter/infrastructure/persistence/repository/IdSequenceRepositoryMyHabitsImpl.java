package com.ovelin.mall.id.generator.starter.infrastructure.persistence.repository;

import com.ovelin.mall.common.sharding.core.ov.ShardId;
import com.ovelin.mall.id.generator.starter.autoconfigure.IdGeneratorProperties;
import com.ovelin.mall.id.generator.starter.domain.module.Segment;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.SequenceName;
import com.ovelin.mall.id.generator.starter.domain.repository.SequenceRepository;

import com.ovelin.mall.id.generator.starter.infrastructure.persistence.mapper.IdSequenceMapper;
import com.ovelin.mall.id.generator.starter.infrastructure.persistence.po.IdSequencePO;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class IdSequenceRepositoryMyHabitsImpl implements SequenceRepository {
    private IdSequenceMapper idSequenceMapper;
    private IdGeneratorProperties properties;
    public IdSequenceRepositoryMyHabitsImpl(IdSequenceMapper idSequenceMapper, IdGeneratorProperties properties) {
        this.idSequenceMapper = idSequenceMapper;
        this.properties = properties;
    }

    /**
     * 原子地分配一个新号段。
     * 方法返回时，分配结果已持久化提交，与调用方的事务无关。
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Segment allocateSegment(SequenceName name, ShardId shardId) {
        int shardValue = shardId.value();
        IdSequencePO sequence = idSequenceMapper.selectByName(name.value(), shardValue);
        if (sequence == null) {
            idSequenceMapper.insertIfAbsent(name.value(), shardValue);
        }

        sequence = idSequenceMapper.selectByName(name.value(), shardValue);


        Segment segment = Segment.fromStartAndSize(
                sequence.nextValue(),
                properties.getAllocationSize());
        int updatedRows = idSequenceMapper.advance(name.value(), shardValue, properties.getAllocationSize());
        if (updatedRows != 1) {
            throw new IllegalStateException(
                    "Failed to advance sequence: " + name.value() + " with shardId: " + shardId);
        }
        return segment;
    }

}
