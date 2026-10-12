package com.ovelin.mall.common.web.starter.exceptionHandler;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/probe")
public class ProbeController {

    public record Body(@NotBlank String name, @Min(1) Integer age) {}

    public static class BusinessConflictException extends RuntimeException {
        public BusinessConflictException() { super("conflict, internal detail 42"); }
    }

    @GetMapping("/items/{id}")
    public String item(@PathVariable Long id) { return "ok"; }

    @PutMapping(value = "/body", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void body(@RequestBody @Valid Body body) {}

    @PostMapping("/post-only")
    public void postOnly() {}

    @GetMapping("/boom")
    public void boom() { throw new IllegalStateException("secret detail"); }

    @GetMapping("/iae")
    public void iae() { throw new IllegalArgumentException("secret iae"); }

    @GetMapping("/conflict")
    public void conflict() { throw new BusinessConflictException(); }
}