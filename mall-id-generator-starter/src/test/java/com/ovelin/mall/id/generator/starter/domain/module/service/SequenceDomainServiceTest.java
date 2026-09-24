package com.ovelin.mall.id.generator.starter.domain.module.service;

import com.ovelin.mall.id.generator.starter.benchmark.TestSequenceRepository;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.SequenceName;
import com.ovelin.mall.id.generator.starter.domain.service.SequenceDomainService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SequenceDomainServiceTest {

    @Test
    void testNextId() {
        System.out.println("Starting testNextId...");
        for (int thread = 0; thread < 3; thread++) {
            for (int loopCount = 0; loopCount < 3; loopCount++) {
                testNextId(2 << thread, (loopCount + 1) * 100);
            }
        }
    }

    @Test
    void testNextValues() {
        System.out.println("Starting testNextValues...");
        for (int thread = 0; thread < 3; thread++) {
            for (int loopCount = 0; loopCount < 3; loopCount++) {
                testNextValues(2 << thread, (loopCount + 1) * 100);
            }
        }
    }

    void testNextValues(int threadsCount, int loopCountPerThread) {
        SequenceDomainService sequenceDomainService = new SequenceDomainService(new TestSequenceRepository());
        AtomicInteger allocationCount = new AtomicInteger();
        TestResult testResult = test(threadsCount, loopCountPerThread, (x, y) ->{
            int count = ThreadLocalRandom.current().nextInt(1, 100);
            allocationCount.addAndGet(count);
            return sequenceDomainService.nextValues(
                    new SequenceName("test-sequence"),
                    count
            ).toArray();
        });
        AtomicInteger errorCount = testResult.errorCount();
        ConcurrentMap<Long, Integer> result = testResult.result();
        assertEquals(0, errorCount.get(), "There were " + errorCount.get() + " errors during the test.");
        assertEquals(allocationCount.get(), result.size(), "Expected " + allocationCount.get() + " unique IDs, but got " + result.size());
        System.out.println("Total allocated IDs: " + allocationCount.get());
    }

    void testNextId(int threadsCount, int loopCountPerThread) {
        SequenceDomainService sequenceDomainService = new SequenceDomainService(new TestSequenceRepository());
        TestResult testResult = test(threadsCount, loopCountPerThread, (x, y) -> {
            long id = sequenceDomainService.nextValue(new SequenceName("test-sequence"));
            return new long[]{id};
        });
        AtomicInteger errorCount = testResult.errorCount();
        ConcurrentMap<Long, Integer> result = testResult.result();
        assertEquals(0, errorCount.get(), "There were " + errorCount.get() + " errors during the test.");
        assertEquals(threadsCount * loopCountPerThread, result.size(), "Expected " + (threadsCount * loopCountPerThread) + " unique IDs, but got " + result.size());
        System.out.println("Total allocated IDs: " + result.size());
    }
    record TestResult(AtomicInteger errorCount, ConcurrentMap<Long, Integer> result) {
    }
    TestResult test(int threadsCount, int loopCountPerThread, BiFunction<Integer, Integer, long[]> f) {
        List<Thread> threads = new ArrayList<>();
        AtomicInteger errorCount = new AtomicInteger();
        ConcurrentMap<Long, Integer> result = new ConcurrentHashMap<>();
        Instant start = Instant.now();
        for (var i = new Object() {
            int value = 0;
        }; i.value < threadsCount; i.value++) {
            Thread t = Thread.ofVirtual()
                    .name("my-vt-", i.value)
                    .start(() -> {
                        for (int j = 0; j < loopCountPerThread; j++) {
                            try {
                                for (long id : f.apply(i.value, j)) {
                                    result.put(id, 1);
                                }
                                if (j % 10 == 0) {
                                    Thread.sleep(10);
                                }

                            } catch (IllegalStateException | InterruptedException e) {
                                errorCount.incrementAndGet();
                                throw new RuntimeException(e);
                            }

                        }

                    });
            threads.add(t);
        }
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        Instant end = Instant.now();
        System.out.println("Duration of test with " + threadsCount + " threads, each looping " + loopCountPerThread + " times: " + java.time.Duration.between(start, end).toMillis() + " ms");
        return new TestResult(errorCount, result);

    }

}
