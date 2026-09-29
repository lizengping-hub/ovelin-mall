package com.ovelin.mall.id.generator.starter.domain.repository;

import com.ovelin.mall.common.sharding.core.ov.ShardId;
import com.ovelin.mall.id.generator.starter.domain.module.Segment;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.SequenceName;

public interface SequenceRepository {
    Segment allocateSegment(SequenceName name, ShardId shardId);
}
