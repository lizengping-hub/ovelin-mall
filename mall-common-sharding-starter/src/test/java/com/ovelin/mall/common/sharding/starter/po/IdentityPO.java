package com.ovelin.mall.common.sharding.starter.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户登录身份/凭证表 PO
 * 仅作为数据库映射对象，不承载业务逻辑
 * 对应表：user_identity
 */
@Data
@TableName("user_identity")
public class IdentityPO {

    private Long id;

    private Long userId;

    /** phone / email / wechat / apple / alipay 等 */
    private String identityType;

    /** 该身份下的唯一标识：手机号E164 / 邮箱 / 第三方unionid等 */
    private String normalizedIdentifier;

    /** 1-是，0-否 */
    private Integer isPrimary;

    private LocalDateTime verifiedAt;

    private LocalDateTime lastLoginAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}