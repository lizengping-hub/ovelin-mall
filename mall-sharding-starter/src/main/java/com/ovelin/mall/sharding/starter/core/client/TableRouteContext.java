package com.ovelin.mall.sharding.starter.core.client;

import com.ovelin.mall.sharding.starter.api.router.ResolvedRoute;

public final class TableRouteContext {
    private static final ThreadLocal<ResolvedRoute> ROUTE = new ThreadLocal<>();
    public static void set(ResolvedRoute route) {
        ROUTE.set(route);
    }

    public static ResolvedRoute get() {
        return ROUTE.get();
    }
    public interface UncheckedCloseable extends AutoCloseable {
        @Override
        void close(); // 覆盖掉受检异常声明
    }

    public static UncheckedCloseable use(ResolvedRoute route) {
        ResolvedRoute previous = ROUTE.get();
        ROUTE.set(route);
        return () -> {
            if (previous == null) {
                ROUTE.remove();
            } else {
                ROUTE.set(previous);
            }
        };
    }

}