package com.ovelin.mall.common.web.starter;

import com.ovelin.mall.common.web.starter.longToStringCustomizer.LongToStringCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import tools.jackson.databind.json.JsonMapper;

@AutoConfiguration
@ConditionalOnClass(JsonMapper.class)     // 类路径没有 Jackson 时不生效
public class LongToStringJacksonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    JsonMapperBuilderCustomizer longToStringCustomizer() {
        return new LongToStringCustomizer();
    }
}