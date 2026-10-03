package com.ovelin.mall.id.generator.starter.infrastructure.persistence.repository.it;

import com.ovelin.mall.id.generator.starter.domain.repository.SequenceRepository;
import com.ovelin.mall.id.generator.starter.infrastructure.persistence.repository.SequenceRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Optional;

/**
 * 真实实现 SequenceRepositoryImpl 依赖 Spring 的 @Transactional(REQUIRES_NEW),
 * 直接 new 出来的对象没有事务代理,所以必须通过 Spring 容器拿到(或创建)实例。
 * 数据库使用和生产同类型的 MySQL,而不是 H2。
 */
@SpringBootTest()
public class JdbcSequenceRepositoryIT extends SequenceRepositoryContractIT {

    @Autowired
    SequenceRepository repository;

    @Autowired
    ApplicationContext context;

    @Autowired
    JdbcTemplate jdbcTemplate;

    /** 每个用例从空表开始,保证"状态为空的 repository"这个契约前提成立。 */
    @BeforeEach
    void cleanTable() {
        jdbcTemplate.execute("TRUNCATE TABLE id_sequence"); // TODO: 实际表名
    }

    @Override
    protected SequenceRepository createRepository() {
        return repository;
    }

    /**
     * 同一个数据库上的另一个实例,模拟多节点部署。
     * createBean 会走完 BeanPostProcessor,所以返回的同样是带 @Transactional 代理的实例。
     */
    @Override
    protected Optional<SequenceRepository> createAnotherNode() {
        return Optional.of(context.getAutowireCapableBeanFactory().createBean(SequenceRepositoryImpl.class));
    }
}