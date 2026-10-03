package com.ovelin.mall.id.generator.starter.infrastructure.persistence.repository.it; // TODO: 调整为测试所在的实际包

import com.ovelin.mall.common.sharding.core.ov.ShardId;
import com.ovelin.mall.id.generator.starter.domain.module.Segment;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.Allocation;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.SequenceName;
import com.ovelin.mall.id.generator.starter.domain.repository.SequenceRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * SequenceRepository 的契约测试:任何实现(内存、JDBC ……)都必须满足。
 * 子类只需要提供"一个状态为空的 repository"。
 * <p>
 * 契约里的"连续性"(相邻号段首尾相接、号段内逐个 +1)是当前设计的假设;
 * 如果你允许号段之间有空洞,把 {@link #assertContiguous} 放宽为"严格递增"即可。
 */
abstract class SequenceRepositoryContractIT {

    static final SequenceName ORDER = new SequenceName("order");
    static final SequenceName USER = new SequenceName("user");
    static final ShardId SHARD_1 = ShardId.of(1);
    static final ShardId SHARD_2 = ShardId.of(2);

    /** 防御:防止某个实现返回永远取不空的号段导致测试死循环 */
    static final int MAX_SEGMENT_SIZE = 1_000_000;

    /** 返回一个全新、状态为空的 repository。 */
    protected abstract SequenceRepository createRepository();

    /**
     * 返回"同一份底层存储"上的另一个节点,用来模拟多实例部署或服务重启。
     * 内存实现各实例状态互相独立,不支持,保持默认的 empty,相关用例会被跳过。
     */
    protected Optional<SequenceRepository> createAnotherNode() {
        return Optional.empty();
    }

    // ---------- 单节点 ----------

    @Test
    void segmentsShouldNotOverlap() {
        SequenceRepository repository = createRepository();

        List<List<Long>> segments = allocate(repository, ORDER, SHARD_1, 10);

        assertNoDuplicates(flatten(segments));
    }

    @Test
    void segmentsShouldBeContiguousAndIncreasing() {
        SequenceRepository repository = createRepository();

        List<List<Long>> segments = allocate(repository, ORDER, SHARD_1, 10);

        assertContiguous(segments);
    }

    @Test
    void differentKeysShouldNotAffectEachOther() {
        SequenceRepository repository = createRepository();
        List<List<Long>> orderShard1 = new ArrayList<>();
        List<List<Long>> orderShard2 = new ArrayList<>();
        List<List<Long>> userShard1 = new ArrayList<>();

        // 交错分配:如果 key 没有隔离,某个 key 的号段会被别的 key 抢走一段,出现空洞
        for (int i = 0; i < 3; i++) {
            orderShard1.add(drain(repository.allocateSegment(ORDER, SHARD_1)));
            orderShard2.add(drain(repository.allocateSegment(ORDER, SHARD_2)));
            userShard1.add(drain(repository.allocateSegment(USER, SHARD_1)));
        }

        assertContiguous(orderShard1);
        assertContiguous(orderShard2);
        assertContiguous(userShard1);
    }

    @Test
    void concurrentAllocationShouldNotOverlapOrLoseSegments() throws Exception {
        SequenceRepository repository = createRepository();

        List<Long> values = allocateConcurrently(List.of(repository), 8, 50);

        assertNoDuplicates(values);
        assertNoGaps(values);
    }

    // ---------- 多节点 / 重启(仅支持共享存储的实现) ----------

    @Test
    void anotherNodeShouldContinueAfterFirstWithoutOverlap() {
        SequenceRepository node1 = createRepository();
        SequenceRepository node2 = createAnotherNode().orElse(null);
        assumeTrue(node2 != null, "implementation does not share storage across instances");

        List<List<Long>> segments = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            segments.add(drain(node1.allocateSegment(ORDER, SHARD_1)));
            segments.add(drain(node2.allocateSegment(ORDER, SHARD_1)));
        }

        assertNoDuplicates(flatten(segments));
        assertContiguous(segments);
    }

    @Test
    void concurrentAllocationAcrossNodesShouldNotOverlapOrLoseSegments() throws Exception {
        SequenceRepository node1 = createRepository();
        SequenceRepository node2 = createAnotherNode().orElse(null);
        assumeTrue(node2 != null, "implementation does not share storage across instances");

        List<Long> values = allocateConcurrently(List.of(node1, node2), 8, 50);

        assertNoDuplicates(values);
        assertNoGaps(values);
    }

    // ---------- helpers ----------

    /** 把一个号段完整取空,返回其中所有值(按取出顺序)。 */
    static List<Long> drain(Segment segment) {
        List<Long> values = new ArrayList<>();
        while (true) {
            Allocation allocation = segment.next();
            if (!allocation.hasOne()) {
                break;
            }
            values.add(allocation.start());
            assertTrue(values.size() <= MAX_SEGMENT_SIZE, "segment looks unbounded");
        }
        assertFalse(values.isEmpty(), "a freshly allocated segment must not be empty");
        return values;
    }

    static List<List<Long>> allocate(SequenceRepository repository, SequenceName name,
                                     ShardId shard, int count) {
        List<List<Long>> segments = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            segments.add(drain(repository.allocateSegment(name, shard)));
        }
        return segments;
    }

    static List<Long> flatten(List<List<Long>> segments) {
        return segments.stream().flatMap(List::stream).toList();
    }

    static void assertNoDuplicates(List<Long> values) {
        assertEquals(values.size(), new HashSet<>(values).size(), "duplicate values found");
    }

    /** 全部值合起来必须恰好覆盖一个连续区间:没有重叠,也没有丢失的号段。 */
    static void assertNoGaps(List<Long> values) {
        long min = Collections.min(values);
        long max = Collections.max(values);
        assertEquals(values.size(), max - min + 1, "there are gaps between allocated segments");
    }

    /** 号段内逐个 +1,相邻号段首尾相接。 */
    static void assertContiguous(List<List<Long>> segments) {
        for (List<Long> segment : segments) {
            for (int i = 1; i < segment.size(); i++) {
                assertEquals(segment.get(i - 1) + 1, segment.get(i), "values inside a segment must be consecutive");
            }
        }
        for (int i = 1; i < segments.size(); i++) {
            long previousLast = segments.get(i - 1).getLast();
            long currentFirst = segments.get(i).getFirst();
            assertEquals(previousLast + 1, currentFirst, "segment " + i + " must start right after segment " + (i - 1));
        }
    }

    /**
     * 多线程(可跨多个节点)并发分配号段,返回所有线程取到的全部值。
     * 每个线程只写自己的 list,避免共享可变状态掩盖问题。
     */
    static List<Long> allocateConcurrently(List<SequenceRepository> nodes, int threads,
                                           int segmentsPerThread) throws Exception {
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            try {
                CountDownLatch start = new CountDownLatch(1);
                List<Future<List<Long>>> futures = new ArrayList<>();
                for (int t = 0; t < threads; t++) {
                    SequenceRepository node = nodes.get(t % nodes.size());
                    futures.add(pool.submit(() -> {
                        start.await();
                        List<Long> values = new ArrayList<>();
                        for (int i = 0; i < segmentsPerThread; i++) {
                            values.addAll(drain(node.allocateSegment(ORDER, SHARD_1)));
                        }
                        return values;
                    }));
                }
                start.countDown();

                List<Long> all = new ArrayList<>();
                for (Future<List<Long>> f : futures) {
                    all.addAll(f.get(60, TimeUnit.SECONDS));
                }
                return all;
            } finally {
                pool.shutdownNow();
            }
        }
    }
}