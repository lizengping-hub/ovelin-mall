package com.ovelin.mall.auth.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@TableName("user_mfa_factor")
public class UserMfaFactorPO {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long userId;

    private String factorType;

    /** sms / email 因子引用的 user_identity.id，其余类型为空 */
    private Long identityId;

    private String secretEncrypted;

    private Integer status;

    private Instant verifiedAt;

    private Instant lastUsedAt;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Instant createdAt;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Instant updatedAt;
}