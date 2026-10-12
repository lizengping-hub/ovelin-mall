package com.ovelin.mall.common.mybatis.plus.kernal;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;

@Data
@TableName("t_user")
public class UserPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private Integer age;
    private Integer status;
    @Version
    private int version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}