package com.ovelin.mall.common.web.starter.jackson;

import com.ovelin.mall.common.web.starter.TestApp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = TestApp.class)
@AutoConfigureMockMvc
class LongToStringCustomizerControllerTest {

    @Autowired MockMvc mockMvc;

    @Test
    void responseShouldContainIdAsString() throws Exception {
        mockMvc.perform(get("/echo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.id").value("9007199254740993"))
                .andExpect(jsonPath("$.ids[0]").isString())// 字符串，不是数字
                .andExpect(jsonPath("$.ids[0]").value("9007199254740993"))
                .andExpect(jsonPath("$.count").isNumber())
                .andExpect(jsonPath("$.count").value(7));                    // Integer 不受影响
    }

    @Test
    void requestShouldAcceptStringId() throws Exception {
        mockMvc.perform(post("/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"9007199254740993\",\"ids\":[\"1\",\"2\"],\"count\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.id").value("9007199254740993"))
                .andExpect(jsonPath("$.ids[1]").isString())// 字符串，不是数字
                .andExpect(jsonPath("$.ids[1]").value("2"));
    }

    @Test
    void requestShouldStillAcceptNumericId() throws Exception {
        mockMvc.perform(post("/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":123,\"ids\":[],\"count\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.id").value("123"));
    }
}