package com.ovelin.mall.id.generator.starter.application.command;

import com.ovelin.mall.id.generator.starter.application.dto.BusinessType;

import java.util.Objects;

/**
 * Command for requesting ID generation from application layer.
 * Prefer businessType in API; this command currently carries technical SequenceName
 * but can be extended to include tenant, count, traceId, etc.
 */
public final class IdGenerationCommand {
    private final BusinessType businessType; // external/business semantic identifier (preferred)
    private final String tenantId;     // optional
    private final int count;          // how many ids requested
    private final String traceId;     // optional trace id

    private IdGenerationCommand(Builder b) {
        this.businessType = new BusinessType(b.businessType);
        this.tenantId = b.tenantId;
        this.count = b.count;
        this.traceId = b.traceId;
        if (this.count <= 0) throw new IllegalArgumentException("count must be >= 1");
    }

    public BusinessType businessType() { return businessType; }
    public String tenantId() { return tenantId; }
    public int count() { return count; }
    public String traceId() { return traceId; }

    public static Builder builder(String businessType) { return new Builder(businessType); }

    public static final class Builder {
        private final String businessType;
        private String tenantId;
        private int count = 1;
        private String traceId;

        public Builder(String businessType) {
            this.businessType = businessType;
        }

        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder count(int count) { this.count = count; return this; }
        public Builder traceId(String traceId) { this.traceId = traceId; return this; }
        public IdGenerationCommand build() { return new IdGenerationCommand(this); }
    }
}
