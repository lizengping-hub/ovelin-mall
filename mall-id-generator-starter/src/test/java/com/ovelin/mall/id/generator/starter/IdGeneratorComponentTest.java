package com.ovelin.mall.id.generator.starter;

import com.ovelin.mall.common.sharding.core.api.ShardSelector;
import com.ovelin.mall.common.sharding.core.ov.ShardId;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.id.generator.api.IdGenerationCommand;
import com.ovelin.mall.id.generator.api.IdGenerator;
import com.ovelin.mall.id.generator.starter.infrastructure.autoconfigure.IdGeneratorProperties;
import com.ovelin.mall.id.generator.starter.domain.repository.SequenceRepository;
import com.ovelin.mall.id.generator.starter.domain.service.IdGeneratorDomainService;
import com.ovelin.mall.id.generator.starter.domain.service.SequenceDomainService;
import com.ovelin.mall.id.generator.starter.infrastructure.persistence.repository.MemorySequenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
/**
 * 组件测试:真实组装整条链路,只把随机分片换成固定分片,保证结果可复现。
 * 	真实链路,固定分片:默认/指定分片、多段递增、nextIds 批量、并发唯一
 */
class IdGeneratorComponentTest {

    static final ShardId DEFAULT_SHARD = ShardId.of(3);

    IdGenerator idGenerator;

    @BeforeEach
    void setUp() {
        IdGeneratorProperties properties = new IdGeneratorProperties(100);
        SequenceRepository repository = new MemorySequenceRepository(properties);
        ShardSelector resolver = mock(ShardSelector.class);
        when(resolver.select()).thenReturn(DEFAULT_SHARD);

        idGenerator = new IdGeneratorAppService(
                new IdGeneratorDomainService(new SequenceDomainService(repository), resolver));
    }

    @Test
    void shouldUseResolverShardWhenNotSpecified() {
        ShardedId id = idGenerator.nextId(IdGenerationCommand.builder("order").build());

        assertEquals(DEFAULT_SHARD, id.shardId());
    }

    @Test
    void shouldUseSpecifiedShard() {
        ShardedId id = idGenerator.nextId(IdGenerationCommand.builder("order").shardId(9).build());

        assertEquals(9, id.shardId().value());
    }

    @Test
    void sequenceShouldIncreaseWithinSameShardAcrossSegments() {
        long prev = -1;
        IdGenerationCommand command = IdGenerationCommand.builder("order").shardId(1).build();
        for (int i = 0; i < 350; i++) { // 容量 100,跨多个段
            ShardedId id = idGenerator.nextId(command);
            long seq = id.sequence();
            assertEquals(seq, prev + 1);
            prev = seq;
        }
    }

    @Test
    void nextIdsShouldReturnRequestedCountInOneShardWithUniqueIds() {
        List<ShardedId> ids = idGenerator.nextIds(
                IdGenerationCommand.builder("order").shardId(2).count(250).build());

        assertEquals(250, ids.size());
        Set<Long> unique = new HashSet<>();
        for (ShardedId id : ids) {
            assertEquals(2, id.shardId().value());
            assertTrue(unique.add(id.value()));
        }
    }

    @Test
    void idsShouldBeUniqueOnPlatformThreads() throws Exception {
        assertUniqueUnderConcurrency(() -> Executors.newFixedThreadPool(8), 8, 2_000);
    }

    @Test
    void idsShouldBeUniqueOnVirtualThreads() throws Exception {
        assertUniqueUnderConcurrency(Executors::newVirtualThreadPerTaskExecutor, 200, 200);
    }

    private void assertUniqueUnderConcurrency(Supplier<ExecutorService> poolFactory,
                                              int tasks, int perTask) throws Exception {
        Set<Long> ids = ConcurrentHashMap.newKeySet();
        try (ExecutorService pool = poolFactory.get()) {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < tasks; t++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    for (int i = 0; i < perTask; i++) {
                        ids.add(idGenerator.nextId(IdGenerationCommand.builder("order").build()).value());
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> f : futures) {
                f.get(30, TimeUnit.SECONDS);
            }
        }
        assertEquals(tasks * perTask, ids.size());
    }
}
