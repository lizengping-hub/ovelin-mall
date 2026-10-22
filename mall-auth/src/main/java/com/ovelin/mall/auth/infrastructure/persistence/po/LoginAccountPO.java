package com.ovelin.mall.auth.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.Instant;


@Data
@TableName("login_account")
public class LoginAccountPO {

    @TableId(type = IdType.INPUT)
    private Long userId;

    @Version
    private long version;

    /** 由数据库 DEFAULT CURRENT_TIMESTAMP(3) 维护，只读 */
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Instant createdAt;

    /** 由数据库 ON UPDATE CURRENT_TIMESTAMP(3) 维护，只读 */
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Instant updatedAt;
}