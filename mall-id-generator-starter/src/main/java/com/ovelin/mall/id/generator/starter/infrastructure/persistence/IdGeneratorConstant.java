package com.ovelin.mall.id.generator.starter.infrastructure.persistence;

import com.ovelin.mall.sharding.starter.api.router.ShardGroupKey;

public class IdGeneratorConstant {
    public final static ShardGroupKey SHARED_GROUP_KEY = new ShardGroupKey("id-generator");
    public final static String ID_SEQUENCE_TABLE_NAME = "id_sequence";
}
