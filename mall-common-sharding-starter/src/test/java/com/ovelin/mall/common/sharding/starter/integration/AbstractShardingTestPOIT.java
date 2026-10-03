package com.ovelin.mall.common.sharding.starter.integration;

import com.ovelin.mall.common.sharding.core.api.DataSourceIndexResolver;
import com.ovelin.mall.common.sharding.core.api.TableIndexResolver;
import com.ovelin.mall.common.sharding.starter.integration.mapper.ShardingTestIdentityMapper;
import com.ovelin.mall.common.sharding.starter.integration.mapper.ShardingTestUserMapper;
import com.ovelin.mall.common.sharding.starter.integration.po.ShardingTestIdentityPO;
import com.ovelin.mall.common.sharding.starter.integration.po.ShardingTestUserPO;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Map;

abstract class AbstractShardingTestPOIT {
    @Autowired
    ShardingTestUserMapper userMapper;
    @Autowired
    ShardingTestIdentityMapper userIdentityMapper;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    TableIndexResolver tableIndexResolver;
    @Autowired
    DataSourceIndexResolver dataSourceIndexResolver;

    static final RowMapper<ShardingTestIdentityPO> IDENTITY_ROW_MAPPER =
            (rs, rowNum) -> {
                ShardingTestIdentityPO po = new ShardingTestIdentityPO();
                po.setId(rs.getLong("id"));
                po.setUserId(rs.getLong("user_id"));
                po.setIdentityType(rs.getString("identity_type"));
                po.setIdentifier(rs.getString("identifier"));
                return po;
            };
    static final RowMapper<ShardingTestUserPO> USER_ROW_MAPPER =
            (rs, rowNum) -> {
                ShardingTestUserPO po = new ShardingTestUserPO();
                po.setId(rs.getLong("id"));
                po.setDisplayName(rs.getString("display_name"));
                return po;
            };


    Map<String, JdbcTemplate> unshardedJdbcTemplates = Map.of(
            "identity-ds0", createUnshardedJdbcTemplate(3308, "sharding_test_identity_database", "identity-ds0"),
            "user-ds0", createUnshardedJdbcTemplate(3308, "sharding_test_user_database", "user-ds0"),
            "identity-ds1", createUnshardedJdbcTemplate(3309, "sharding_test_identity_database", "identity-ds1"),
            "user-ds1", createUnshardedJdbcTemplate(3309, "sharding_test_user_database", "user-ds1")
    );

    static JdbcTemplate createUnshardedJdbcTemplate(int port, String database, String instanceKey) {
        String jdbcUrl = "jdbc:mysql://127.0.0.1:" + port + "/" + database + "?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC\n";
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername("ovelin");
        config.setPassword("ovelin");
        config.setPoolName("HikariPool-Unsharded-" + instanceKey);
        HikariDataSource dataSource = new HikariDataSource(config);
        return new JdbcTemplate(dataSource);

    }


    @BeforeEach
    void cleanTables() {
        jdbcTemplate.execute("TRUNCATE TABLE sharding_test_identity");
        jdbcTemplate.execute("TRUNCATE TABLE sharding_test_user");
    }
}
