package com.ovelin.mall.auth.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@TableName("user_password")
public class UserPasswordPO {

    @TableId(type = IdType.INPUT)
    private Long userId;

    private String encodedPassword;

    private Instant passwordChangedAt;

    private int failedAttempts;

    @TableField(updateStrategy=FieldStrategy.ALWAYS)
    private Instant lockedUntil;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Instant createdAt;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Instant updatedAt;
}