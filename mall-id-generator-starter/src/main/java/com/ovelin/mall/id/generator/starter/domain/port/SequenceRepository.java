package com.ovelin.mall.id.generator.starter.domain.port;

import com.ovelin.mall.id.generator.starter.domain.module.valueobject.AllocationSize;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.Segment;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.SequenceName;

public interface SequenceRepository {
    Segment nextSegment(SequenceName name);
    void initIfAbsent(SequenceName name, AllocationSize value);
}
