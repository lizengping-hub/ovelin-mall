package com.ovelin.mall.id.generator.starter.application;

import com.ovelin.mall.id.generator.starter.domain.module.ov.SequenceName;

public interface IdGenerator {
    long nextId(SequenceName name);
}
