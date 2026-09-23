package com.ovelin.mall.id.generator.starter;

import com.ovelin.mall.id.generator.starter.domain.module.ov.AllocationSize;
import com.ovelin.mall.id.generator.starter.domain.module.ov.Segment;
import com.ovelin.mall.id.generator.starter.domain.module.ov.SequenceName;
import com.ovelin.mall.id.generator.starter.domain.port.SequenceRepository;
import com.ovelin.mall.sharding.starter.api.ShardedJdbcExecutor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MallIdGeneratorStarterApplicationTests {
    @Autowired
    ShardedJdbcExecutor shardTransactionExecutor;
    @Autowired
    SequenceRepository sequenceRepository;
    @Test
    void contextLoads() {
        if (shardTransactionExecutor == null) {
            throw new IllegalStateException("shardTransactionExecutor is null");
        }
        if (sequenceRepository == null) {
            throw new IllegalStateException("sequenceRepository is null");
        }
        shardTransactionExecutor.execute(IdGeneratorConstant.SHARED_GROUP_KEY, (jdbcTemplate, route) -> {
            jdbcTemplate.execute("SELECT * FROM " + route.resolveTableName(IdGeneratorConstant.ID_SEQUENCE_TABLE_NAME));
            return null;
        });
        sequenceRepository.initIfAbsent(new SequenceName("test_sequence"), new AllocationSize(1000));
    }

    @Test
    void testShardedJdbcExecutor() {
        if (shardTransactionExecutor == null) {
            throw new IllegalStateException("shardTransactionExecutor is null");
        }
        shardTransactionExecutor.execute(IdGeneratorConstant.SHARED_GROUP_KEY, (jdbcTemplate, route) -> {
            jdbcTemplate.execute("SELECT * FROM " + route.resolveTableName(IdGeneratorConstant.ID_SEQUENCE_TABLE_NAME));
            return null;
        });
    }
    @Test
    void testSequenceRepository() {
        if (sequenceRepository == null) {
            throw new IllegalStateException("sequenceRepository is null");
        }
        int allocationSize = 1000;
        sequenceRepository.initIfAbsent(new SequenceName("test_sequence"), new AllocationSize(allocationSize));
        Segment lastSegment = null;
        for (int i = 0; i < 10; i++) {
            Segment segment = sequenceRepository.nextSegment(new SequenceName("test_sequence"));
            System.out.println("Next segment: " + segment);
            if (lastSegment != null) {
                if (segment.minValue() != lastSegment.minValue() + lastSegment.size()) {
                    throw new IllegalStateException("Segments are not contiguous last: " + lastSegment + " and new " + segment);
                }
            }

            lastSegment = segment;

        }


    }


}
