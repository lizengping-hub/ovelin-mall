package com.ovelin.mall.common.web.starter;

import com.ovelin.mall.common.web.starter.exceptionHandler.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class GlobalExceptionHandlerAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    GlobalExceptionHandler globalExceptionHandler(){
        return new GlobalExceptionHandler();
    }
}
