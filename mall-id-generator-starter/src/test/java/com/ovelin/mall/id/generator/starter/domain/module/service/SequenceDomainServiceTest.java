package com.ovelin.mall.id.generator.starter.domain.module.service;

import com.ovelin.mall.common.sharding.core.ov.ShardId;
import com.ovelin.mall.id.generator.starter.infrastructure.autoconfigure.IdGeneratorProperties;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.Allocations;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.SequenceName;
import com.ovelin.mall.id.generator.starter.domain.service.SequenceDomainService;
import com.ovelin.mall.id.generator.starter.infrastructure.persistence.repository.MemorySequenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 	跨段递增、按需换段、不同分片/名称互相独立、nextValues 数量准确且不重叠、并发唯一
 */
class SequenceDomainServiceTest {

    /** 假设 MemorySequenceRepository(n) 的 n 是每个 Segment 的容量;小容量便于覆盖换段逻辑 */
    static final IdGeneratorProperties ID_GENERATOR_PROPERTIES = new IdGeneratorProperties(5);
    /** 假设 MemorySequenceRepository(n) 的 n 是每个 Segment 的容量;小容量便于覆盖换段逻辑 */
    static final int SEGMENT_SIZE = ID_GENERATOR_PROPERTIES.allocationSize();

    // TODO: 按 SequenceName 的实际构造方式调整(new / of)
    static final SequenceName ORDER = new SequenceName("order");
    static final SequenceName USER = new SequenceName("user");
    static final ShardId SHARD_1 = ShardId.of(1);
    static final ShardId SHARD_2 = ShardId.of(2);

    MemorySequenceRepository repository;
    SequenceDomainService service;

    @BeforeEach
    void setUp() {
        repository = spy(new MemorySequenceRepository(ID_GENERATOR_PROPERTIES));
        service = new SequenceDomainService(repository);
    }

    // ---------- nextValue ----------

    @Test
    void nextValueShouldBeStrictlyIncreasingAcrossSegments() {
        long prev = -1;
        for (int i = 0; i < SEGMENT_SIZE * 5 + 2; i++) {
            long v = service.nextValue(ORDER, SHARD_1);
            assertEquals(prev + 1, v, "expected " + v + " == " + (prev + 1));
            prev = v;
        }
    }

    @Test
    void shouldAllocateSegmentLazilyOnlyWhenExhausted() {
        for (int i = 0; i < SEGMENT_SIZE; i++) {
            service.nextValue(ORDER, SHARD_1);
        }
        verify(repository, times(1)).allocateSegment(ORDER, SHARD_1);

        service.nextValue(ORDER, SHARD_1); // 第一个段用完后的下一次调用才换段
        verify(repository, times(2)).allocateSegment(ORDER, SHARD_1);
    }

    @Test
    void differentShardsAndNamesShouldHaveIndependentSequences() {
        List<Long> order1 = take(ORDER, SHARD_1, 12);
        List<Long> order2 = take(ORDER, SHARD_2, 12);
        List<Long> user1 = take(USER, SHARD_1, 12);

        assertEquals(order1, order2);
        assertEquals(order1, user1);
    }

    // ---------- nextValues ----------

    @Test
    void nextValuesShouldReturnExactlyCountAcrossSegments() {
        int count = SEGMENT_SIZE * 3 + 2;

        Allocations allocations = service.nextValues(ORDER, SHARD_1, count);
        List<Long> values = allocations.toList();

        assertEquals(count, allocations.idCount());
        assertEquals(count, values.size());
        assertEquals(count, new HashSet<>(values).size(), "values must be unique");

        long next = service.nextValue(ORDER, SHARD_1);
        assertTrue(next > Collections.max(values), "following value must not overlap");
    }

    @Test
    void nextValueAndNextValuesShouldNotOverlap() {
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            assertTrue(seen.add(service.nextValue(ORDER, SHARD_1)));
            for (Long v : service.nextValues(ORDER, SHARD_1, 4).toList()) {
                assertTrue(seen.add(v), "duplicate: " + v);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void nextValuesShouldRejectNonPositiveCount(int count) {
        assertThrows(IllegalArgumentException.class,
                () -> service.nextValues(ORDER, SHARD_1, count));
        verifyNoInteractions(repository);
    }

    // ---------- concurrency ----------

    @Test
    void nextValueShouldBeUniqueAndWasteNoSegmentsUnderConcurrency() throws Exception {
        int threads = 8, perThread = 5_000;
        Set<Long> all = ConcurrentHashMap.newKeySet();
        AtomicInteger duplicates = new AtomicInteger();

        runConcurrently(threads, () -> {
            for (int i = 0; i < perThread; i++) {
                if (!all.add(service.nextValue(ORDER, SHARD_1))) {
                    duplicates.incrementAndGet();
                }
            }
        });

        int total = threads * perThread;
        assertEquals(0, duplicates.get());
        assertEquals(total, all.size());
        // total 能被 SEGMENT_SIZE 整除:双重检查正确时,分配的段数应恰好等于 total / SEGMENT_SIZE
        verify(repository, times(total / SEGMENT_SIZE)).allocateSegment(ORDER, SHARD_1);
    }

    @Test
    void nextValuesShouldBeUniqueUnderConcurrency() throws Exception {
        int threads = 8, calls = 300, batch = 7;
        Set<Long> all = ConcurrentHashMap.newKeySet();
        AtomicInteger duplicates = new AtomicInteger();

        runConcurrently(threads, () -> {
            for (int i = 0; i < calls; i++) {
                for (Long v : service.nextValues(ORDER, SHARD_1, batch).toList()) {
                    if (!all.add(v)) {
                        duplicates.incrementAndGet();
                    }
                }
            }
        });

        assertEquals(0, duplicates.get());
        assertEquals(threads * calls * batch, all.size());
    }

    // ---------- helpers ----------

    private List<Long> take(SequenceName name, ShardId shard, int n) {
        return IntStream.range(0, n).mapToObj(i -> service.nextValue(name, shard)).toList();
    }

    /** 所有线程同时起跑;任何线程里的断言失败/异常都会通过 Future.get 抛出 */
    private void runConcurrently(int threads, Runnable task) throws Exception {
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            try {
                CountDownLatch start = new CountDownLatch(1);
                List<Future<?>> futures = new ArrayList<>();
                for (int t = 0; t < threads; t++) {
                    futures.add(pool.submit(() -> {
                        start.await();
                        task.run();
                        return null;
                    }));
                }
                start.countDown();
                for (Future<?> f : futures) {
                    f.get(30, TimeUnit.SECONDS);
                }
            } finally {
                pool.shutdownNow();
            }
        }
    }
}
