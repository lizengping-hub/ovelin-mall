package com.ovelin.mall.sharding.starter.api;

import com.ovelin.mall.sharding.starter.core.MyBatisClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public interface DatabaseClientManager extends AutoCloseable {

    JdbcTemplate getJdbcTemplate(String instanceKey);

    TransactionTemplate getTransactionTemplate(String instanceKey);

    MyBatisClient getMyBatisClient(String instanceKey);

    @Override
    void close();
}
