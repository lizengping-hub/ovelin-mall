package com.ovelin.mall.id.generator.starter.infrastructure.autoconfigure;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "id-generator")
public record IdGeneratorProperties (
    @Min(value = 1, message = "Allocation size must be greater than 0")
    int allocationSize
){
}
