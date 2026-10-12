package com.ovelin.mall.common.web.starter.jackson;

import com.ovelin.mall.common.web.starter.LongToStringJacksonAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LongToStringCustomizerTest {

    // 超过 2^53，JS 的 Number 会丢精度
    private static final long BIG = 9007199254740993L;

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    LongToStringJacksonAutoConfiguration.class));

    record Payload(Long boxed, long primitive, List<Long> list, Map<String, Long> map,
                   Integer count, Long empty) {}

    @Test
    void shouldSerializeLongAsString() {
        runner.run(ctx -> {
            JsonMapper mapper = ctx.getBean(JsonMapper.class);

            String json = mapper.writeValueAsString(
                    new Payload(BIG, BIG, List.of(BIG), Map.of("k", BIG), 7, null));

            assertThat(json).contains("\"boxed\":\"9007199254740993\"");
            assertThat(json).contains("\"primitive\":\"9007199254740993\"");   // 基本类型 long
            assertThat(json).contains("\"list\":[\"9007199254740993\"]");
            assertThat(json).contains("\"map\":{\"k\":\"9007199254740993\"}");
        });
    }

    @Test
    void shouldNotAffectOtherNumberTypes() {
        runner.run(ctx -> {
            JsonMapper mapper = ctx.getBean(JsonMapper.class);

            String json = mapper.writeValueAsString(new Payload(1L, 1L, List.of(), Map.of(), 7, null));

            assertThat(json).contains("\"count\":7");        // Integer 仍是数字
        });
    }

    @Test
    void shouldKeepNullAsNull() {
        runner.run(ctx -> {
            JsonMapper mapper = ctx.getBean(JsonMapper.class);

            String json = mapper.writeValueAsString(new Payload(1L, 1L, List.of(), Map.of(), 7, null));

            // 不能变成 "null" 字符串。若你的全局配置排除了 null，这里改为 doesNotContain("empty")
            assertThat(json).contains("\"empty\":null");
        });
    }

    @Test
    void shouldDeserializeFromStringAndNumber() {
        runner.run(ctx -> {
            JsonMapper mapper = ctx.getBean(JsonMapper.class);

            Payload fromString = mapper.readValue(
                    "{\"boxed\":\"9007199254740993\",\"primitive\":\"9007199254740993\"}", Payload.class);
            Payload fromNumber = mapper.readValue(
                    "{\"boxed\":123,\"primitive\":123}", Payload.class);

            assertThat(fromString.boxed()).isEqualTo(BIG);
            assertThat(fromString.primitive()).isEqualTo(BIG);
            assertThat(fromNumber.boxed()).isEqualTo(123L);
        });
    }

    @Test
    void shouldBackOffWhenJacksonIsMissing() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(LongToStringJacksonAutoConfiguration.class))
                .withClassLoader(new FilteredClassLoader(JsonMapper.class))
                .run(ctx -> assertThat(ctx).hasNotFailed()
                        .doesNotHaveBean("longToStringCustomizer"));
    }
}