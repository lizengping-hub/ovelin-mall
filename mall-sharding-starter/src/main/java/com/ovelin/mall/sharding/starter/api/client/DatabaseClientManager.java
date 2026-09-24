package com.ovelin.mall.sharding.starter.api.client;

import com.ovelin.mall.sharding.starter.core.client.MyBatisClient;
import com.ovelin.mall.sharding.starter.core.client.MyBatisPlusClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public interface DatabaseClientManager extends AutoCloseable {

    JdbcTemplate getJdbcTemplate(String instanceKey);

    TransactionTemplate getTransactionTemplate(String instanceKey);

    MyBatisClient getMyBatisClient(String instanceKey);

    public MyBatisPlusClient getMyBatisPlusClient(String instanceKey);

    @Override
    void close();
}
