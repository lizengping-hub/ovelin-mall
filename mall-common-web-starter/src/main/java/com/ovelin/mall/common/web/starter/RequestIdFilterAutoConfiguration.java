package com.ovelin.mall.common.web.starter;

import com.ovelin.mall.common.web.starter.filter.RequestIdFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.json.JsonMapper;

@AutoConfiguration
public class RequestIdFilterAutoConfiguration {
    @Bean
    RequestIdFilter requestIdFilter(){
        return new RequestIdFilter();
    }
}
