package com.ovelin.mall.id.generator.api;

import com.ovelin.mall.common.sharding.core.ov.ShardId;

import java.util.Objects;
import java.util.Optional;

/**
 * Command for requesting ID generation from the application layer.
 * <p>
 * Carries application-level input only: {@link BusinessType} (business semantic identifier)
 * and an optional {@link ShardId}. Mapping to the domain's SequenceName is done by the
 * application service, not here.
 */
public final class IdGenerationCommand {
    private final BusinessType businessType;
    private final ShardId shardId;      // null = not specified, the domain side picks one via ShardSelector
    private final String tenantId;      // optional
    private final int count;            // number of ids requested, >= 1
    private final String traceId;       // optional

    private IdGenerationCommand(Builder b) {
        if (b.count <= 0) {
            throw new IllegalArgumentException("count must be >= 1: " + b.count);
        }
        this.businessType = b.businessType;
        this.shardId = b.shardId;
        this.tenantId = b.tenantId;
        this.count = b.count;
        this.traceId = b.traceId;
    }

    public BusinessType businessType() { return businessType; }

    public Optional<ShardId> shardId() { return Optional.ofNullable(shardId); }

    public String tenantId() { return tenantId; }

    public int count() { return count; }

    public String traceId() { return traceId; }

    public static Builder builder(String businessType) {
        return new Builder(new BusinessType(businessType));
    }
    public static Builder builder(BusinessType businessType) {
        return new Builder(businessType);
    }
    public static final class Builder {
        private final BusinessType businessType;
        private ShardId shardId;
        private String tenantId;
        private int count = 1;
        private String traceId;

        private Builder(BusinessType businessType) {
            this.businessType = Objects.requireNonNull(businessType, "businessType");
        }

        /** Raw shard number from the interface layer; null means unspecified. Range is validated by {@link ShardId}. */
        public Builder shardId(Integer shardId) {
            this.shardId = shardId == null ? null : ShardId.of(shardId);
            return this;
        }

        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }

        public Builder count(int count) { this.count = count; return this; }

        public Builder traceId(String traceId) { this.traceId = traceId; return this; }

        public IdGenerationCommand build() { return new IdGenerationCommand(this); }
    }
}