package com.ovelin.mall.id.generator.starter;

import com.ovelin.mall.common.sharding.core.ov.ShardId;
import com.ovelin.mall.id.generator.starter.domain.module.Segment;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.SequenceName;
import com.ovelin.mall.id.generator.starter.domain.repository.SequenceRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

@SpringBootTest
class MallIdGeneratorAppServiceStarterApplicationTests {
    @Autowired
    SequenceRepository sequenceRepository;
    @Test
    void contextLoads() {
        if (sequenceRepository == null) {
            throw new IllegalStateException("sequenceRepository is null");
        }
    }

    @Test
    void testSequenceRepository() throws InterruptedException {
        if (sequenceRepository == null) {
            throw new IllegalStateException("sequenceRepository is null");
        }
        SequenceName name = new SequenceName("test_sequence");

        for (int i = 0; i < 10; i++) {
            ShardId shardId = ShardId.of(1000);
            Segment lastSegment = null;
            for (int j = 0; j < 10; j++) {
                Segment segment = sequenceRepository.allocateSegment(name, shardId);
                System.out.println("Next segment: " + segment);
                if (lastSegment != null) {
                    if (segment.minValue() != lastSegment.minValue() + lastSegment.size()) {
                        throw new IllegalStateException("Segments are not contiguous last: " + lastSegment + " and new " + segment);
                    }
                }
                lastSegment = segment;
                System.out.println("Current time: " +Instant.now().toString());

            }
        }
    }
}
