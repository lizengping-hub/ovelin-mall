package com.ovelin.mall.common.web.starter.exceptionHandler;

import com.ovelin.mall.common.exception.DomainException;
import com.ovelin.mall.common.exception.global.InvalidArgumentException;
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
    public static class BusinessDomainException extends DomainException {
        public BusinessDomainException() { super(()->"BUSINESS_DOMAIN_EXCEPTION","test business domain exception message"); }
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

    @GetMapping("/business/demain/exception")
    public void businessDomainException() { throw new BusinessDomainException(); }

    @GetMapping("/exception/domain/global")
    public void globalDomainException() { throw new InvalidArgumentException("test invalid argument exception message"); }
}