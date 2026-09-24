package benchmark;


import com.ovelin.mall.id.generator.starter.domain.module.valueobject.Allocations;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.SequenceName;
import com.ovelin.mall.id.generator.starter.domain.service.SequenceDomainService;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode({Mode.Throughput, Mode.SampleTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class SequenceDomainServiceBenchmark {

    SequenceDomainService service;
    SequenceName seq;

    @Setup(Level.Trial)
    public void setup() {
        service = new SequenceDomainService(new TestSequenceRepository());
        seq = new SequenceName("test-sequence");
    }

    @Benchmark
    @Threads(1)
    public long nextValue_t1() { return service.nextValue(seq); }

    @Benchmark
    @Threads(16)
    public long nextValue_t16() { return service.nextValue(seq); }

    @Benchmark
    @Threads(64)
    public long nextValue_t64() { return service.nextValue(seq); }

    @Benchmark
    @Threads(1)
    public Allocations nextValues_batch32_t1() { return service.nextValues(seq, 32); }

    @Benchmark
    @Threads(16)
    public Allocations nextValues_batch32_t16() { return service.nextValues(seq, 32); }

    @Benchmark
    @Threads(64)
    public Allocations nextValues_batch32_t64() { return service.nextValues(seq, 32); }

    @Benchmark
    @Threads(16)
    public Allocations nextValues_batch1_t16() { return service.nextValues(seq, 1); }

    // 多 key 场景,分散争用
    @State(Scope.Thread)
    public static class ThreadLocalKey {
        SequenceName key = new SequenceName("seq-" + ThreadLocalRandom.current().nextInt(100));
    }

    @Benchmark
    @Threads(16)
    public long nextValue_multiKey(ThreadLocalKey tk) {
        return service.nextValue(tk.key);
    }
}
