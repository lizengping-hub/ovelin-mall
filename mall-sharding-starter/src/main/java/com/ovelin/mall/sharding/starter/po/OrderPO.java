package com.ovelin.mall.sharding.starter.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_order")
public class OrderPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

