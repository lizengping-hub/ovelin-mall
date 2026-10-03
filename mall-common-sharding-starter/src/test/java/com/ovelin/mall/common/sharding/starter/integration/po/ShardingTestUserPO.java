package com.ovelin.mall.common.sharding.starter.integration.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户资料表 PO
 * 仅作为数据库映射对象，不承载业务逻辑，不做值对象层面的校验
 * 对应表：sharding_test_user
 */
@Data
@TableName("sharding_test_user")
public class ShardingTestUserPO {
    private Long id;
    private String displayName;

}