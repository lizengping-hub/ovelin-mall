package com.ovelin.mall.user.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.Instant;

/**
 * 用户资料表 PO
 * 仅作为数据库映射对象，不承载业务逻辑，不做值对象层面的校验
 * 对应表：user
 */



/**
 * 可空字段（头像、生日、注销时间）必须 updateStrategy = ALWAYS：
 * MyBatis-Plus 的 updateById 默认忽略 null，不加的话"清除头像/生日"和注销脱敏都不会落库。
 */
@Data
@TableName("user")
public class UserPO {

    private Long id;

    private String displayName;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String avatarUrl;

    /** 0-未知，1-男，2-女 */
    private Integer gender;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer birthYear;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer birthMonth;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer birthDay;

    /** 0-禁用，1-正常，2-注销 */
    private Integer status;

    private String registerSource;

    @Version
    private long version;

    /** 注销时间（业务时间），未注销为 null；普通列，随 status 一起写入 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Instant deregisteredAt;

    /** 由数据库 DEFAULT CURRENT_TIMESTAMP(3) 维护，只读 */
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Instant createdAt;

    /** 由数据库 ON UPDATE CURRENT_TIMESTAMP(3) 维护，只读 */
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Instant updatedAt;
}