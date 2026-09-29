package com.ovelin.mall.id.generator.starter.infrastructure.persistence;

import com.ovelin.mall.id.generator.starter.autoconfigure.IdGeneratorProperties;
import com.ovelin.mall.id.generator.starter.domain.repository.SequenceRepository;
import com.ovelin.mall.id.generator.starter.infrastructure.persistence.repository.MemorySequenceRepository;


/** 内存实现:每个实例状态独立,所以不覆盖 createAnotherNode,多节点用例会被自动跳过。 */
class MemorySequenceRepositoryTest extends SequenceRepositoryContractTest {

    @Override
    protected SequenceRepository createRepository() {
        IdGeneratorProperties properties = new IdGeneratorProperties(100); // TODO: 如果参数不是号段容量,按实际调整
        return new MemorySequenceRepository(properties);
    }
}