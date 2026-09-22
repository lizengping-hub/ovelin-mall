package com.ovelin.mall.id.generator.starter.domain.port;

import com.ovelin.mall.id.generator.starter.domain.module.ov.AllocationSize;
import com.ovelin.mall.id.generator.starter.domain.module.ov.Segment;
import com.ovelin.mall.id.generator.starter.domain.module.ov.SequenceName;

public interface SequenceRepository {
    Segment nextSegment(SequenceName name);
    void initIfAbsent(SequenceName name, AllocationSize value);
}
