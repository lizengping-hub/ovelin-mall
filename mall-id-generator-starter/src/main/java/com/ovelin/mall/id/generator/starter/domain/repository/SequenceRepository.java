package com.ovelin.mall.id.generator.starter.domain.repository;

import com.ovelin.mall.id.generator.starter.domain.module.valueobject.AllocationSize;
import com.ovelin.mall.id.generator.starter.domain.module.Segment;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.SequenceName;

public interface SequenceRepository {
    Segment allocateSegment(SequenceName name);
    void initIfAbsent(SequenceName name, AllocationSize value);
}
