package com.ovelin.mall.common.mybatis.plus.kernal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SqlTestConfiguration {

    @Bean
    public SqlCollectingInterceptor sqlCollectingInterceptor() {
        return new SqlCollectingInterceptor();
    }
}