package com.ovelin.mall.id.generator.starter.application.assembler;

import com.ovelin.mall.id.generator.starter.application.dto.IdDTO;
import com.ovelin.mall.sharding.starter.api.ShardedId;

public class IdAssembler {
    private IdAssembler(){}
    public static IdDTO toIdDTO(ShardedId id) {
        return new IdDTO(id.getId());
    }
    public static ShardedId toShardedId(IdDTO id) {
        return ShardedId.fromId(id.id());
    }
}
