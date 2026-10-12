package com.ovelin.mall.common.mybatis.plus.kernal;

import lombok.Getter;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

@Intercepts({
    @Signature(
        type = StatementHandler.class,
        method = "prepare",
        args = {Connection.class, Integer.class}
    )
})
@Getter
public class SqlCollectingInterceptor implements Interceptor {

    private final List<String> sqls = new ArrayList<>();

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler handler =
                (StatementHandler) invocation.getTarget();

        sqls.add(handler.getBoundSql().getSql());

        return invocation.proceed();
    }

    public void clear() {
        sqls.clear();
    }
}