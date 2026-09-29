package com.ovelin.mall.id.generator.starter.autoconfigure;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "id-generator")
public class IdGeneratorProperties {
    private String repository;
    @Min(value = 1, message = "Allocation size must be greater than 0")
    private int allocationSize;

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public int getAllocationSize() {
        return allocationSize;
    }

    public void setAllocationSize(int allocationSize) {
        this.allocationSize = allocationSize;
    }
}
