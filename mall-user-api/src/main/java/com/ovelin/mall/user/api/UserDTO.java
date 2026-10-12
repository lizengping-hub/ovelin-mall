package com.ovelin.mall.user.api;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("user")
public class UserDTO {
    private Long id;

    private String displayName;

    private String avatarUrl;

    /** 0-未知，1-男，2-女 */
    private Integer gender;

    private Integer birthYear;

    private Integer birthMonth;

    private Integer birthDay;

    /** 1-正常，0-禁用，2-注销 */
    private Integer status;

    private String registerSource;

    private Instant deregisteredAt;

    private Instant createdAt;


    private Instant updatedAt;

}