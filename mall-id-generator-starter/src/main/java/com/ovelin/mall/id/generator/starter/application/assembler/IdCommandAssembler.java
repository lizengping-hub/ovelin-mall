package com.ovelin.mall.id.generator.starter.application.assembler;

import com.ovelin.mall.id.generator.starter.application.command.IdGenerationCommand;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.SequenceName;

/**
 * Simple mapper from application command to domain SequenceName.
 * Place more advanced routing/strategy here when needed.
 */
public final class IdCommandAssembler {

    private IdCommandAssembler() {}

    public static SequenceName toSequenceName(IdGenerationCommand cmd) {
        String business = cmd.businessType().value();
        if (business == null || business.isBlank()) throw new IllegalArgumentException("businessType required");
        // Default: use businessType as sequence name. Change logic if technical names differ.
        return new SequenceName(business);
    }
}
