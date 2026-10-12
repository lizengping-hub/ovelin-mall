package com.ovelin.mall.user.interfaces.web;

import com.ovelin.mall.user.api.UserDTO;
import com.ovelin.mall.user.api.UserQueryService;
import com.ovelin.mall.user.application.service.UserApplicationService;
import com.ovelin.mall.user.domain.exception.UserNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/users/{id}")
@RequiredArgsConstructor
public class UserController {

    private final UserApplicationService userApplicationService;

    private final UserQueryService userQueryService;

    @GetMapping
    public UserDTO findById(@PathVariable Long id){
        UserDTO dto = userQueryService.findById(id);
        if (dto == null) {
            throw new UserNotFoundException(id);
        }
        return dto;
    }

    @PutMapping("/display-name")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateDisplayName(@PathVariable Long id,
                                  @RequestBody @Valid UpdateDisplayNameRequest req) {
        userApplicationService.updateDisplayName(id, req.displayName());
    }

    @PutMapping("/birthday")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateBirthday(@PathVariable Long id,
                               @RequestBody @Valid UpdateBirthdayRequest req) {
        userApplicationService.updateBirthDate(id, req.year(), req.month(), req.day());
    }

    @PutMapping("/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateAvatar(@PathVariable Long id,
                             @RequestBody @Valid UpdateAvatarRequest req) {
        userApplicationService.updateAvatarUrl(id, req.avatarUrl());
    }

    @PutMapping("/gender")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateGender(@PathVariable Long id,
                             @RequestBody @Valid UpdateGenderRequest req) {
        userApplicationService.updateGender(id, req.gender());
    }

    @PostMapping("/deregister")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deregister(@PathVariable Long id) {
        userApplicationService.deregister(id, Instant.now());
    }

    public record UpdateDisplayNameRequest(@NotBlank String displayName) {}

    public record UpdateBirthdayRequest(Integer year,                       // 可空
                                        @NotNull @Min(1) @Max(12) Integer month,
                                        @NotNull @Min(1) @Max(31) Integer day) {}

    public record UpdateAvatarRequest(@NotBlank String avatarUrl) {}

    public record UpdateGenderRequest(@NotNull Integer gender) {}
}