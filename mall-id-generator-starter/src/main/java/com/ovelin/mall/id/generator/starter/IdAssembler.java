package com.ovelin.mall.id.generator.starter;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.id.generator.api.BusinessType;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.SequenceName;

public class IdAssembler {
    private IdAssembler(){}

    public static SequenceName toSequenceName(BusinessType businessType) {
        return SequenceName.of(businessType.value());
    }
}
