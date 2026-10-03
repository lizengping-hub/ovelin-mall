package com.ovelin.mall.common.sharding.starter.integration.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户登录身份/凭证表 PO
 * 仅作为数据库映射对象，不承载业务逻辑
 * 对应表：sharding_test_identity
 */
@Data
@TableName("sharding_test_identity")
public class ShardingTestIdentityPO {

    private Long id;

    private Long userId;

    /** phone / email / wechat / apple / alipay 等 */
    private String identityType;

    /** 该身份下的唯一标识：手机号E164 / 邮箱 / 第三方unionid等 */
    private String identifier;
}