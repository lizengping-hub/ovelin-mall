package com.ovelin.mall.id.generator.api;

import com.ovelin.mall.common.sharding.core.ov.ShardId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * builder 校验
 */
class IdGenerationCommandTest {

    @Test
    void shouldApplyDefaults() {
        IdGenerationCommand command = IdGenerationCommand.builder("order").build();

        assertEquals(1, command.count());
        assertTrue(command.shardId().isEmpty());
        assertNull(command.tenantId());
        assertNull(command.traceId());
    }

    @Test
    void shouldCarryAllFields() {
        IdGenerationCommand command = IdGenerationCommand.builder("order")
                .shardId(4).tenantId("t1").count(10).traceId("trace-1").build();

        assertEquals(ShardId.of(4), command.shardId().orElseThrow());
        assertEquals("t1", command.tenantId());
        assertEquals(10, command.count());
        assertEquals("trace-1", command.traceId());
    }

    @Test
    void nullShardIdShouldMeanUnspecified() {
        IdGenerationCommand command = IdGenerationCommand.builder("order").shardId(null).build();

        assertTrue(command.shardId().isEmpty());
    }

    @Test
    void shouldRejectShardOutOfRange() {
        assertThrows(IllegalArgumentException.class,
                () -> IdGenerationCommand.builder("order").shardId(-1));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -5})
    void shouldRejectNonPositiveCount(int count) {
        assertThrows(IllegalArgumentException.class,
                () -> IdGenerationCommand.builder("order").count(count).build());
    }
}
