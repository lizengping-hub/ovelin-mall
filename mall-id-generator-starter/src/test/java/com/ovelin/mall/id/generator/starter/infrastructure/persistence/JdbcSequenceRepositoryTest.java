package com.ovelin.mall.id.generator.starter.infrastructure.persistence;

import com.ovelin.mall.common.sharding.core.api.ShardResolver;
import com.ovelin.mall.common.sharding.core.core.RandomShardResolver;
import com.ovelin.mall.id.generator.starter.domain.repository.SequenceRepository;
import com.ovelin.mall.id.generator.starter.infrastructure.persistence.repository.SequenceRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

/**
 * 真实实现 SequenceRepositoryImpl 依赖 Spring 的 @Transactional(REQUIRES_NEW),
 * 直接 new 出来的对象没有事务代理,所以必须通过 Spring 容器拿到(或创建)实例。
 * 数据库使用和生产同类型的 MySQL,而不是 H2。
 */
@SpringBootTest(classes = JdbcSequenceRepositoryTest.TestApp.class)
@EnableAutoConfiguration
@Testcontainers
public class JdbcSequenceRepositoryTest extends SequenceRepositoryContractTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("shared_database");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        // TODO: 建表脚本(表名、列名以 IdSequenceMapper 的 SQL 为准),放在 src/test/resources 下
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.schema-locations", () -> "classpath:id_sequence.sql");
        // TODO: 号段容量的配置项,前缀以 IdGeneratorProperties 的 @ConfigurationProperties 为准
        registry.add("id-generator.allocation-size", () -> "100");
    }

    /** 最小化的测试应用:靠 starter 自己的自动配置装配 Mapper、事务和 SequenceRepositoryImpl。 */
    @EnableAutoConfiguration
    static class TestApp {
        @Bean
        ShardResolver shardResolver() {
            return new RandomShardResolver();
        }
    }

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