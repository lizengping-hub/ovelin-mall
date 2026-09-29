CREATE TABLE `user_address` (
    `id`           BIGINT       NOT NULL COMMENT '地址 ID',
    `user_id`      BIGINT       NOT NULL COMMENT '用户 ID',
    `receiver`     VARCHAR(64)  NOT NULL COMMENT '收件人',
    `phone`        VARCHAR(20)  NOT NULL COMMENT '收件人电话',
    `province`     VARCHAR(32)  NOT NULL COMMENT '省',
    `city`         VARCHAR(32)  NOT NULL COMMENT '市',
    `district`     VARCHAR(32)  NOT NULL COMMENT '区',
    `detail`       VARCHAR(256) NOT NULL COMMENT '详细地址',
    `is_default`   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户地址';