package com.ovelin.mall.common.web.starter;

import com.ovelin.mall.common.web.starter.exceptionHandler.GlobalDomainExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class GlobalDomainExceptionHandlerAutoConfiguration {
    @Bean
    GlobalDomainExceptionHandler globalDomainExceptionHandler(){
        return new GlobalDomainExceptionHandler();
    }
}
