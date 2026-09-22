package com.ovelin.mall.id.generator.starter.domain.module.aggregate;

import com.ovelin.mall.id.generator.starter.domain.port.SequenceRepository;

public class Sequence {
    private final SequenceRepository sequenceRepository;
    public Sequence(SequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }
}
