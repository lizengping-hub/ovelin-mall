package com.ovelin.mall.id.generator.starter.application;

import com.ovelin.mall.id.generator.starter.domain.module.valueobject.SequenceName;

public interface IdGenerator {
    long nextId(SequenceName name);
}
