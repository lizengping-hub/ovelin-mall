package com.ovelin.mall.id.generator.starter.application;

import com.ovelin.mall.id.generator.starter.domain.module.valueobject.SequenceName;

public class IdGeneratorImpl implements IdGenerator{

    private final IdSequenceManager idSequenceManager;

    public IdGeneratorImpl(IdSequenceManager idSequenceManager) {
        this.idSequenceManager = idSequenceManager;
    }

    @Override
    public long nextId(SequenceName name) {
        return 0;
    }
}
