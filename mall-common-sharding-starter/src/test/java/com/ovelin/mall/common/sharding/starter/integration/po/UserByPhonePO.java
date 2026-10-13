package com.ovelin.mall.common.sharding.starter.integration.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_by_phone")
public class UserByPhonePO {
    Long userId;
    String phone;
}
