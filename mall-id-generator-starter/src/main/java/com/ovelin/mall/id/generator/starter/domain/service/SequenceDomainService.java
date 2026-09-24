package com.ovelin.mall.id.generator.starter.domain.service;

import com.ovelin.mall.id.generator.starter.domain.module.Segment;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.Allocation;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.Allocations;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.SequenceName;
import com.ovelin.mall.id.generator.starter.domain.repository.SequenceRepository;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

public class SequenceDomainService {
    SequenceRepository sequenceRepository;
    ConcurrentMap<SequenceName, ReentrantLock> locks = new ConcurrentHashMap<>();
    ConcurrentMap<SequenceName, Segment> segments = new ConcurrentHashMap<>();

    public SequenceDomainService(SequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }
    public long nextValue(SequenceName sequenceName) {
        // fast path: try without locking
        Segment segment = segments.get(sequenceName);
        if (segment != null) {
            Allocation allocation = segment.next();
            if (allocation.hasOne()) {
                return allocation.start();
            }
        }

        ReentrantLock lock = locks.computeIfAbsent(sequenceName, k -> new ReentrantLock());
        lock.lock();
        try {
            while (true){
                Segment current = segments.get(sequenceName);

                if (current != null && current != segment) {
                    Allocation allocation = current.next();
                    if (allocation.hasOne()) {
                        return allocation.start();
                    }
                }
                Segment newSegment = sequenceRepository.allocateSegment(sequenceName);
                segments.put(sequenceName, newSegment);
                Allocation allocation = segments.get(sequenceName).next();
                if (allocation.hasOne()) {
                    return allocation.start();
                }
                segment = segments.get(sequenceName); // 这个新段也已耗尽,记录下来,继续走创建流程
            }
        } finally {
            lock.unlock();
        }
    }

    public Allocations nextValues(SequenceName sequenceName, int count) {
        if (count <= 0) throw new IllegalArgumentException("count must be > 0");
        Allocations allocations = new Allocations();
        while (allocations.idCount() < count) {
            Segment segment = segments.get(sequenceName);

            if (segment != null) {
                Allocation a = segment.nextN(count - allocations.idCount());
                if (a.count() > 0) {
                    allocations.add(a);
                    continue; // 段还没耗尽就够了;若耗尽了拿到不足的部分,继续下一轮循环补齐
                }
            }

            // 走到这里说明当前段不存在或已耗尽,需要换新段
            ReentrantLock lock = locks.computeIfAbsent(sequenceName, k -> new ReentrantLock());
            lock.lock();
            try {
                Segment latest = segments.get(sequenceName);
                // 用引用比较判断:如果这期间已经有别的线程把新段换上了,直接跳过,回到循环重试即可
                if (latest == segment) {
                    Segment newSegment = sequenceRepository.allocateSegment(sequenceName);
                    segments.put(sequenceName, newSegment);
                }
            } finally {
                lock.unlock();
            }
            // 不在这里直接分配,回到 while 循环开头重新走一遍完整流程
        }
        return allocations;
    }
}
