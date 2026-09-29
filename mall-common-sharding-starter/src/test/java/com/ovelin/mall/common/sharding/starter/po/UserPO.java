package com.ovelin.mall.common.sharding.starter.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户资料表 PO
 * 仅作为数据库映射对象，不承载业务逻辑，不做值对象层面的校验
 * 对应表：user
 */
@Data
@TableName("user")
public class UserPO {

    private Long id;

    private String username;

    /** 冗余字段，仅用于展示/通知，不作登录判定依据，真实凭证以 UserIdentityPO 为准 */
    private String phoneE164;

    private String phoneRawInput;

    private String email;

    private String avatarUrl;

    /** 0-未知，1-男，2-女 */
    private Integer gender;

    private LocalDate birthday;

    /** 1-正常，0-禁用，2-注销 */
    private Integer status;

    private String registerSource;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 软删除标记，MyBatis-Plus 逻辑删除，值为 null 表示未删除 */
    @TableLogic(value = "null", delval = "now()")
    private LocalDateTime deletedAt;
}