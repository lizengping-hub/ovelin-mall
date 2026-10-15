package com.ovelin.mall.common.web.starter.exceptionHandler;

import com.ovelin.mall.common.exception.global.face.GlobalErrorCode;
import com.ovelin.mall.common.web.starter.TestApp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = TestApp.class)
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @Nested
    @DisplayName("客户端错误：统一格式，文案固定，不回显输入")
    class ClientErrors {

        @Test
        void typeMismatch() throws Exception {
            mockMvc.perform(get("/probe/items/{id}", "abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
                    .andExpect(jsonPath("$.detail", not(containsString("For input string"))))
                    .andExpect(jsonPath("$.detail", not(containsString("java.lang"))));
        }

        @Test
        void malformedJson() throws Exception {
            var perform = mockMvc.perform(put("/probe/body")
                    .contentType(MediaType.APPLICATION_JSON).content("{oops"));
            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
                    .andExpect(jsonPath("$.detail", not(containsString("oops"))));
        }

        @Test
        void validationFailureIncludesFieldNames() throws Exception {
            var perform = mockMvc.perform(put("/probe/body")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\" \",\"age\":0}"));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
                    .andExpect(jsonPath("$.detail", containsString("name ")))
                    .andExpect(jsonPath("$.detail", containsString("age ")));
        }

        @Test
        void methodNotAllowedKeepsAllowHeader() throws Exception {
            var perform = mockMvc.perform(get("/probe/post-only"));
            perform.andExpect(status().isMethodNotAllowed())
                    .andExpect(header().string("Allow", containsString("POST")))
                    .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
        }

        @Test
        void unsupportedMediaType() throws Exception {
            mockMvc.perform(put("/probe/body")
                            .contentType(MediaType.TEXT_PLAIN).content("x"))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
        }

        @Test
        void unknownPathDoesNotEchoPath() throws Exception {
            var perform = mockMvc.perform(get("/no-such-path-xyz"));
            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.detail", containsString("no-such-path-xyz")));
        }
    }

    @Nested
    @DisplayName("兜底：未预期异常")
    class Unexpected {

        @Test
        void unexpectedExceptionBecomes500WithoutDetails() throws Exception {
            var perform = mockMvc.perform(get("/probe/boom"));
            perform.andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                    .andExpect(jsonPath("$.detail", not(containsString("secret"))));
        }

        @Test
        @DisplayName("原生 IllegalArgumentException 不再被当成客户端错误")
        void illegalArgumentIsServerError() throws Exception {
            mockMvc.perform(get("/probe/iae"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.detail", not(containsString("secret"))));
        }

        @Test
        @DisplayName("业务 advice 优先于兜底")
        void businessAdviceWinsOverFallback() throws Exception {
            mockMvc.perform(get("/probe/conflict"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"));
        }

        @Test
        @DisplayName("GlobalDomainExceptionHandler 优先于兜底")
        void invalidArgument() throws Exception {
            mockMvc.perform(get("/probe/exception/domain/global"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(GlobalErrorCode.INVALID_ARGUMENT.name()))
                    .andExpect(jsonPath("$.detail").value("test invalid argument exception message"));
        }
        @Test
        @DisplayName("GlobalDomainExceptionHandler 优先于兜底拦截所有DomainException")
        void businessDomainException() throws Exception {
            mockMvc.perform(get("/probe/business/demain/exception"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("BUSINESS_DOMAIN_EXCEPTION"))
                    .andExpect(jsonPath("$.detail").value("服务器内部错误"));
        }

    }

    @Nested
    @DisplayName("日志级别")
    class Logging {

        @Test
        void serverErrorLogsErrorWithStack(CapturedOutput output) throws Exception {
            mockMvc.perform(get("/probe/boom")).andExpect(status().isInternalServerError());

            assertThat(output).contains("ERROR").contains("IllegalStateException: secret detail");
        }

        @Test
        void clientErrorLogsWarnWithoutStack(CapturedOutput output) throws Exception {
            mockMvc.perform(get("/probe/items/{id}", "abc")).andExpect(status().isBadRequest());

            assertThat(output).contains("WARN").doesNotContain("\tat ");
        }
    }
}