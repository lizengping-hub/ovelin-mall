package com.ovelin.mall.auth.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@TableName("user_identity")
public class UserIdentityPO {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long userId;

    private String identityType;

    private String identifier;

    @TableField("is_primary")
    private Boolean primary;

    private Instant verifiedAt;

    private Instant lastLoginAt;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Instant createdAt;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Instant updatedAt;
}