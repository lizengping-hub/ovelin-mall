package com.ovelin.mall.id.generator.starter.application.port.out;


import com.ovelin.mall.id.generator.starter.application.command.IdGenerationCommand;
import com.ovelin.mall.id.generator.starter.application.dto.IdDTO;

import java.util.List;

public interface IdGenerator {
    List<IdDTO> nextIds(IdGenerationCommand command);
    IdDTO nextId(IdGenerationCommand command);
}
