CREATE TABLE `user` (
    `id`                BIGINT UNSIGNED NOT NULL COMMENT '主键ID',

    -- 展示信息，不作登录凭证
    `username`          VARCHAR(50)  NULL COMMENT '用户昵称/显示名，仅展示',
    `avatar_url`        VARCHAR(500) NULL COMMENT '头像URL',
    `gender`            TINYINT      NOT NULL DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
    `birthday`          DATE         NULL COMMENT '生日',

    -- 冗余通知渠道：从 user_identity 里挑一个主要的同步过来，方便直接查询展示，不作为登录判定依据
    `phone_e164`        VARCHAR(20)  NULL COMMENT '主手机号（冗余，用于通知展示）',
    `email`             VARCHAR(254) NULL COMMENT '主邮箱（冗余，用于通知展示）',

    `status`            TINYINT      NOT NULL DEFAULT 1 COMMENT '账号状态：1-正常，0-禁用，2-注销',
    `register_source`   VARCHAR(20)  NULL COMMENT '注册来源：如 h5、app、wechat_mini 等',

    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`        DATETIME     NULL COMMENT '软删除时间，NULL表示未删除',

    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '用户资料表';

