package com.ovelin.mall.id.generator.api;



import java.util.List;

public interface IdGenerator {
    List<IdDTO> nextIds(IdGenerationCommand command);
    IdDTO nextId(IdGenerationCommand command);
}
