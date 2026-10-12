package com.ovelin.mall.common.web.starter.longToStringCustomizer;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

public class LongToStringCustomizer implements JsonMapperBuilderCustomizer {
    @Override
    public void customize(JsonMapper.Builder jsonMapperBuilder) {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        jsonMapperBuilder.addModules(module);
    }
}
