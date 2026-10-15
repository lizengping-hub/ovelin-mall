package com.ovelin.mall.auth.infrastructure.persistence;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.id.generator.api.IdGenerationCommand;
import com.ovelin.mall.id.generator.api.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@SpringBootTest
public abstract class AuthIntegrationTestSupport {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected IdGenerator idGenerator;

    @BeforeEach
    void cleanTables() {
        for (String table : List.of("user_identity", "identity_lookup", "user_password", "user_mfa_factor", "user")) {
            jdbcTemplate.update("delete from " + table);
        }
    }

    /** 生成落在指定分片的 id，保证走分片路由时能解码出分片 */
    protected ShardedId newId(String businessType, int shard) {
        return idGenerator.nextId(IdGenerationCommand.builder(businessType).shardId(shard).build());
    }

    /** TIMESTAMP(3) 只有毫秒精度，断言前统一截断 */
    protected static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }
}