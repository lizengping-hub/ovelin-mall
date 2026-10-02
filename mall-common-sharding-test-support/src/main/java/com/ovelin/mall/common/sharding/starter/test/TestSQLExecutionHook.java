package com.ovelin.mall.common.sharding.starter.test;

import org.apache.shardingsphere.database.connector.core.jdbcurl.parser.ConnectionProperties;
import org.apache.shardingsphere.infra.executor.sql.hook.SQLExecutionHook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class TestSQLExecutionHook implements SQLExecutionHook {

    private static final List<ActualSql> ACTUAL_SQLS =
            Collections.synchronizedList(new ArrayList<>());

    public static void clear() {
        ACTUAL_SQLS.clear();
    }

    public static List<ActualSql> getActualSqls() {
        synchronized (ACTUAL_SQLS) {
            return List.copyOf(ACTUAL_SQLS);
        }
    }

    @Override
    public void start(
            String dataSourceName,
            String sql,
            List<Object> params,
            ConnectionProperties connectionProps,
            boolean isTrunkThread) {

        ACTUAL_SQLS.add(
                new ActualSql(
                        dataSourceName,
                        sql,
                        params
                )
        );
    }

    @Override
    public void finishSuccess() {
    }

    @Override
    public void finishFailure(Exception cause) {
    }

    public record ActualSql(
            String dataSourceName,
            String sql,
            List<Object> params
    ) {
    }
}