package com.ovelin.mall.sharding.starter.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户资料表 PO
 * 仅作为数据库映射对象，不承载业务逻辑，不做值对象层面的校验
 * 对应表：test_user
 */
@Data
@TableName("test_user")
public class UserPO {
    public static final Instant NOT_DELETED = Instant.EPOCH; // 1970-01-01T00:00:00Z


    private Long id;

    private String displayName;

    /** 冗余字段，仅用于展示/通知，不作登录判定依据，真实凭证以 UserIdentityPO 为准 */
    private String phoneE164;

    private String phoneRawInput;

    private String email;

    private String avatarUrl;

    /** 0-未知，1-男，2-女 */
    private Integer gender;

    private Integer birthYear;

    private Integer birthMonth;

    private Integer birthDay;

    /** 1-正常，0-禁用，2-注销 */
    private Integer status;

    private String registerSource;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Instant createdAt;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Instant updatedAt;

    @TableLogic(value = "'1970-01-01 00:00:00.000'", delval = "now(3)")
    private Instant deletedAt;

    public boolean deleted() {
        return deletedAt != null && !NOT_DELETED.equals(deletedAt);
    }
}