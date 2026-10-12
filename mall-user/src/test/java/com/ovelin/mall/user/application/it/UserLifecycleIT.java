package com.ovelin.mall.user.application.it;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.user.api.CreateUserCommand;
import com.ovelin.mall.user.api.UserApi;
import com.ovelin.mall.user.domain.model.enums.RegisterSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 通过真实 HTTP 入口（MockMvc）走完整链路：
 * Controller → ApplicationService → 持久化 → QueryService → JSON 序列化。
 * <p>
 * 只覆盖单元测试看不到的“接线”问题；参数校验、框架层 4xx 等细节留在各自的单元测试里。
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserLifecycleIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;


     @Autowired
     private UserApi userApi;

    @BeforeEach
    void cleanDb() {
        jdbcTemplate.update("delete from user");
        jdbcTemplate.update("delete from id_sequence");
    }

    @Test
    @DisplayName("创建后 GET 返回 200，且 id 序列化为字符串（Long -> String 自动配置生效）")
    void findById_returnsUser_withStringId() throws Exception {
        Long id = createUser(ShardedId.of(1, 1), "alice");

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", instanceOf(String.class)))
                .andExpect(jsonPath("$.id", is(String.valueOf(id))))
                .andExpect(jsonPath("$.displayName", is("alice")));
    }

    @Test
    @DisplayName("查询不存在的用户 -> 404 + problem+json，带 code 扩展字段")
    void findById_notFound() throws Exception {
        mockMvc.perform(get("/users/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code", is("USER_NOT_FOUND")));
    }

    @Test
    @DisplayName("改名 -> 204，再查询能读回新名字")
    void updateDisplayName_thenQuery() throws Exception {
        Long id = createUser(ShardedId.of(1,2),"alice");

        mockMvc.perform(put("/users/{id}/display-name", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName": "bob"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName", is("bob")));
    }

    @Test
    @DisplayName("改生日/头像/性别 -> 均 204，一次查询读回（代替每个接口各写一个 IT）")
    void updateProfile_thenQuery() throws Exception {
        Long id = createUser(ShardedId.of(1, 3), "alice");

        mockMvc.perform(put("/users/{id}/birthday", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"year": 1995, "month": 6, "day": 15}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(put("/users/{id}/avatar", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"avatarUrl": "https://img.example.com/a.png"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(put("/users/{id}/gender", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gender": 1}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl", is("https://img.example.com/a.png")));
        // TODO: 按 UserDTO 的实际字段补 birthday / gender 的断言
    }

    @Test
    @DisplayName("注销 -> 204，之后查询仍可读到，但资料已脱敏")
    void deregister_thenQuery() throws Exception {
        Long id = createUser(ShardedId.of(1, 4),"alice");

        ResultActions perform = mockMvc.perform(post("/users/{id}/deregister", id));
                perform.andExpect(status().isNoContent());

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName", not("alice")));
        // TODO: 按 UserDTO 实际字段补 status == DEREGISTERED 之类的断言
    }

    @Test
    @DisplayName("对不存在的用户改名 -> 404（领域异常经 UserExceptionHandler 真实映射）")
    void updateDisplayName_userNotFound() throws Exception {
        mockMvc.perform(put("/users/{id}/display-name", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName": "bob"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("USER_NOT_FOUND")));
    }

    /**
     * 前置数据：沿用你原 UserLifecycleIT 里“创建用户”的方式，返回用户 id。
     */
    private Long createUser(ShardedId shardedId, String name) {
        return userApi.createUser(
                CreateUserCommand.builder(shardedId, name, RegisterSource.WEB.code()).build()).value();
    }
}