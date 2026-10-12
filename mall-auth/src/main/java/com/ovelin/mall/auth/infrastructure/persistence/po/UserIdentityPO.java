package com.ovelin.mall.auth.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 用户登录身份/凭证表 PO
 * 仅作为数据库映射对象，不承载业务逻辑
 * 对应表：user_identity
 */
@Data
@TableName("user_identity")
public class UserIdentityPO {
    private Long id;

    private Long userId;

    /** phone / email / wechat / apple / alipay 等 */
    private String identityType;

    /** 该身份下的唯一标识：手机号E164 / 邮箱 / 第三方unionid等 */
    private String identifier;

    /** 凭证信息，如密码哈希；验证码登录场景可为空 */
    private String credential;

    /** 1-是，0-否 */
    private Integer isPrimary;

    private Instant verifiedAt;

    private Instant lastLoginAt;

    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
}