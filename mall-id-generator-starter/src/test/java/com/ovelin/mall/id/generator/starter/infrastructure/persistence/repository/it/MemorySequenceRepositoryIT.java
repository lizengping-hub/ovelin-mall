package com.ovelin.mall.id.generator.starter.infrastructure.persistence.repository.it;

import com.ovelin.mall.id.generator.starter.infrastructure.autoconfigure.IdGeneratorProperties;
import com.ovelin.mall.id.generator.starter.domain.repository.SequenceRepository;
import com.ovelin.mall.id.generator.starter.infrastructure.persistence.repository.MemorySequenceRepository;


/** 内存实现:每个实例状态独立,所以不覆盖 createAnotherNode,多节点用例会被自动跳过。 */
class MemorySequenceRepositoryIT extends SequenceRepositoryContractIT {

    @Override
    protected SequenceRepository createRepository() {
        IdGeneratorProperties properties = new IdGeneratorProperties(1000);
        return new MemorySequenceRepository(properties);
    }
}