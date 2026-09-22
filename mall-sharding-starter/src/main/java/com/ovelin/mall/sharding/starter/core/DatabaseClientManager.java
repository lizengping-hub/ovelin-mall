package com.ovelin.mall.sharding.starter.core;

import com.ovelin.mall.sharding.starter.api.ShardingProperties;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseClientManager implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(DatabaseClientManager.class);

    private final ShardingProperties properties;
    private final Map<String, DatabaseClient> clients = new ConcurrentHashMap<>();

    public DatabaseClientManager(ShardingProperties properties) {
        this.properties = properties;
    }

    JdbcTemplate getJdbcTemplate(String instanceKey) {
        return getClient(instanceKey).jdbcTemplate();
    }

    TransactionTemplate getTransactionTemplate(String instanceKey) {
        return getClient(instanceKey).transactionTemplate();
    }

    public MyBatisClient getMyBatisClient(String instanceKey) {
        return getClient(instanceKey).myBatisClient();
    }

    private DatabaseClient getClient(String instanceKey) {
        return clients.computeIfAbsent(instanceKey, this::createClient);
    }

    private DatabaseClient createClient(String instanceKey) {
        ShardingProperties.Instance instance =
                properties.instances().get(instanceKey);
        if (instance == null) {
            throw new IllegalArgumentException("Unknown database instance: " + instanceKey);
        }

        String jdbcUrl = "jdbc:mysql://" + instance.host() + ":" + instance.port()
                + "/"
                + "?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC";
        log.debug("JDBC URL: {}", jdbcUrl);
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(instance.username());
        dataSource.setPassword(instance.password());
        dataSource.setMaximumPoolSize(instance.maximumPoolSize());
        dataSource.setPoolName("identity-" + instanceKey);
        log.info("Created database connection pool for instance {}", instanceKey);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        DataSourceTransactionManager transactionManager =
                new DataSourceTransactionManager(dataSource);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        MyBatisClient myBatisClient = new MyBatisClient(
                dataSource,
                "com.ovelin.mall");

        return new DatabaseClient(
                dataSource,
                jdbcTemplate,
                 transactionTemplate,myBatisClient);
    }

    @Override
    public void close() {
        clients.values().forEach(client -> {
            HikariDataSource dataSource = client.dataSource();
            log.info("Closing database connection pool {}", dataSource.getPoolName());
            dataSource.close();
        });
        clients.clear();
    }

    private record DatabaseClient(
            HikariDataSource dataSource,
            JdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate,
            MyBatisClient myBatisClient) {
    }

}
