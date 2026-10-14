package com.ovelin.mall.user.interfaces.web;

import com.ovelin.mall.common.web.starter.GlobalExceptionHandlerAutoConfiguration;
import com.ovelin.mall.common.web.starter.LongToStringJacksonAutoConfiguration;
import com.ovelin.mall.user.api.UserDTO;
import com.ovelin.mall.user.api.UserQueryService;
import com.ovelin.mall.user.application.service.UserAppService;
import com.ovelin.mall.user.domain.exception.ErrorCode;
import com.ovelin.mall.user.domain.exception.InvalidArgumentException;
import com.ovelin.mall.user.domain.exception.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;   // Boot 4 的新包名
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({LongToStringJacksonAutoConfiguration.class, GlobalExceptionHandlerAutoConfiguration.class})
class UserControllerTest {

    private static final long ID = 123456789L;

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    UserAppService userApplicationService;
    @MockitoBean
    UserQueryService userQueryService;

    private static String json(String s) {
        return s.replace('\'', '"');   // 测试里用单引号写 JSON，省去转义
    }

    @Nested
    @DisplayName("GET /users/{id}")
    class FindById {
        @Test
        void shouldReturn200WithBody() throws Exception {
            UserDTO dto = new UserDTO();              // 按你的 UserDTO 实际构造方式调整
            dto.setId(ID);
            dto.setDisplayName("Tom");
            dto.setCreatedAt(Instant.now().truncatedTo(ChronoUnit.MILLIS));
            when(userQueryService.findById(ID)).thenReturn(dto);

            var res = mockMvc.perform(get("/users/{id}", ID));
            res.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isString())
                    .andExpect(jsonPath("$.id").value(String.valueOf(ID)))
                    .andExpect(jsonPath("$.displayName").value("Tom"));
        }

        @Test
        void shouldReturn404WhenNotFound() throws Exception {
            when(userQueryService.findById(ID)).thenReturn(null);


            mockMvc.perform(get("/users/{id}", ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
                    .andExpect(jsonPath("$.detail").value(ErrorCode.USER_NOT_FOUND.clientMessage()));
        }

    }

    @Nested
    @DisplayName("PUT /users/{id}/display-name")
    class UpdateDisplayName {
        @Test
        void shouldReturn204AndDelegate() throws Exception {
            mockMvc.perform(put("/users/{id}/display-name", ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("{'displayName':'Andy'}")))
                    .andExpect(status().isNoContent());

            verify(userApplicationService).updateDisplayName(ID, "Andy");
        }

        @Test
        void shouldReturn400WhenBlank() throws Exception {
            mockMvc.perform(put("/users/{id}/display-name", ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("{'displayName':'  '}")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(userApplicationService);
        }

        @Test
        void shouldReturn400WhenFieldMissing() throws Exception {
            mockMvc.perform(put("/users/{id}/display-name", ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(userApplicationService);
        }

        @Test
        void shouldReturn404WhenUserNotFound() throws Exception {
            doThrow(new UserNotFoundException(ID))
                    .when(userApplicationService).updateDisplayName(ID, "Andy");

            mockMvc.perform(put("/users/{id}/display-name", ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("{'displayName':'Andy'}")))
                    .andExpect(status().isNotFound());
        }


        @Test
        void shouldReturn400WhenDomainRejectsValue() throws Exception {
            doThrow(new InvalidArgumentException("Display name too loog"))
                    .when(userApplicationService).updateDisplayName(eq(ID), any());

            mockMvc.perform(put("/users/{id}/display-name", ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("{'displayName':'x'}")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
                    .andExpect(jsonPath("$.detail").value("Display name too loog"));
            // 参数错误：放行领域异常里写给用户的文案

        }
    }

    @Nested
    @DisplayName("PUT /users/{id}/birthday")
    class UpdateBirthday {
        @Test
        void shouldDelegateWithYear() throws Exception {
            mockMvc.perform(put("/users/{id}/birthday", ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("{'year':1990,'month':5,'day':20}")))
                    .andExpect(status().isNoContent());

            verify(userApplicationService).updateBirthDate(ID, 1990, 5, 20);
        }

        @Test
        void shouldAllowNullYear() throws Exception {
            mockMvc.perform(put("/users/{id}/birthday", ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("{'month':5,'day':20}")))
                    .andExpect(status().isNoContent());

            verify(userApplicationService).updateBirthDate(ID, null, 5, 20);
        }

        @Test
        void shouldReturn400WhenMonthOutOfRange() throws Exception {
            mockMvc.perform(put("/users/{id}/birthday", ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("{'month':13,'day':1}")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(userApplicationService);
        }

        @Test
        void shouldReturn400WhenDayMissing() throws Exception {
            mockMvc.perform(put("/users/{id}/birthday", ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("{'month':5}")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(userApplicationService);
        }
    }

    @Nested
    @DisplayName("PUT /users/{id}/avatar")
    class UpdateAvatar {
        @Test
        void shouldReturn204AndDelegate() throws Exception {
            mockMvc.perform(put("/users/{id}/avatar", ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("{'avatarUrl':'https://img.example.com/a.png'}")))
                    .andExpect(status().isNoContent());

            verify(userApplicationService).updateAvatarUrl(ID, "https://img.example.com/a.png");
        }

        @Test
        void shouldReturn400WhenBlank() throws Exception {
            mockMvc.perform(put("/users/{id}/avatar", ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("{'avatarUrl':''}")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(userApplicationService);
        }
    }

    @Nested
    @DisplayName("PUT /users/{id}/gender")
    class UpdateGender {
        @Test
        void shouldReturn204AndDelegate() throws Exception {
            mockMvc.perform(put("/users/{id}/gender", ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("{'gender':1}")))
                    .andExpect(status().isNoContent());

            verify(userApplicationService).updateGender(ID, 1);
        }

        @Test
        void shouldReturn400WhenNull() throws Exception {
            mockMvc.perform(put("/users/{id}/gender", ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(userApplicationService);
        }

        @Test
        void shouldReturn400WhenCodeUnknown() throws Exception {
            doThrow(new InvalidArgumentException("unknown gender"))
                    .when(userApplicationService).updateGender(ID, 99);

            mockMvc.perform(put("/users/{id}/gender", ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("{'gender':99}")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("unknown gender"));
        }
    }

    @Nested
    @DisplayName("POST /users/{id}/deregister")
    class Deregister {
        @Test
        void shouldReturn204AndUseServerTime() throws Exception {
            mockMvc.perform(post("/users/{id}/deregister", ID))
                    .andExpect(status().isNoContent());

            ArgumentCaptor<Instant> at = ArgumentCaptor.forClass(Instant.class);
            verify(userApplicationService).deregister(eq(ID), at.capture());
            assertThat(at.getValue()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
        }

        @Test
        void shouldReturn404WhenUserNotFound() throws Exception {
            doThrow(new UserNotFoundException(ID))
                    .when(userApplicationService).deregister(eq(ID), any());

            mockMvc.perform(post("/users/{id}/deregister", ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value(ErrorCode.USER_NOT_FOUND.clientMessage()));
        }

        @Test
        void shouldRejectWrongHttpMethod() throws Exception {
            mockMvc.perform(get("/users/{id}/deregister", ID))
                    .andExpect(status().isMethodNotAllowed())
            ;
        }
    }
}