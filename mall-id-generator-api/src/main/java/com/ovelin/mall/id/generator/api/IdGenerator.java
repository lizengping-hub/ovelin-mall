package com.ovelin.mall.id.generator.api;



import com.ovelin.mall.common.sharding.core.ov.ShardedId;

import java.util.List;

public interface IdGenerator {
    List<ShardedId> nextIds(IdGenerationCommand command);
    ShardedId nextId(IdGenerationCommand command);
}
