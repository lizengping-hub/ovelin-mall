
CREATE TABLE user_identity (
       id BIGINT NOT NULL COMMENT '身份记录ID',
       user_id BIGINT NOT NULL COMMENT '用户ID',

       identity_type VARCHAR(20) NOT NULL COMMENT 'username / phone / email',
       normalized_identifier VARCHAR(255) NOT NULL COMMENT '标准化登录标识',

       is_primary TINYINT NOT NULL DEFAULT 0,
       verified_at DATETIME NULL,
       last_login_at DATETIME NULL,

       created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
       updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
           ON UPDATE CURRENT_TIMESTAMP,

       PRIMARY KEY (id),
       UNIQUE KEY uk_normalized_identifier (normalized_identifier),
       KEY idx_user_id (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '用户登录身份';
CREATE TABLE oauth_identity (
    `id`                BIGINT       NOT NULL COMMENT '凭证 ID',
    `user_id`           BIGINT       NOT NULL COMMENT '用户 ID',
    `provider`          VARCHAR(32)  NOT NULL COMMENT '提供商：WECHAT / GITHUB / GOOGLE / APPLE',
    `provider_user_id`  VARCHAR(128) NOT NULL COMMENT '第三方用户 ID（openId）',
    `union_id`          VARCHAR(128) DEFAULT NULL COMMENT '微信 unionId',
    `nickname`          VARCHAR(64)  DEFAULT NULL COMMENT '第三方昵称快照',
    `avatar`            VARCHAR(256) DEFAULT NULL COMMENT '第三方头像快照',
    `status`            VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    `bound_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_login_at`     DATETIME     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_provider_user` (`provider`, `provider_user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方登录凭证';
CREATE TABLE oauth_identity (
        id BIGINT NOT NULL COMMENT '第三方身份记录ID',
        user_id BIGINT NOT NULL COMMENT '用户ID',

        provider VARCHAR(32) NOT NULL COMMENT 'google / github / wechat 等',
        issuer VARCHAR(255) DEFAULT NULL COMMENT 'OIDC issuer',
        provider_subject VARCHAR(255) NOT NULL COMMENT '第三方用户唯一标识',

        display_name VARCHAR(255) DEFAULT NULL,
        email VARCHAR(255) DEFAULT NULL,
        avatar_url VARCHAR(500) DEFAULT NULL,

        verified_at DATETIME DEFAULT NULL,
        last_login_at DATETIME DEFAULT NULL,
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
            ON UPDATE CURRENT_TIMESTAMP,

        PRIMARY KEY (id),
        UNIQUE KEY uk_oauth_identity
            (provider, provider_subject),
        KEY idx_oauth_user_id (user_id)
);

CREATE TABLE user_password (
       user_id BIGINT NOT NULL COMMENT '用户ID',
       password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',

       password_changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
       failed_attempts INT NOT NULL DEFAULT 0,
       locked_until DATETIME NULL,

       created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
       updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
           ON UPDATE CURRENT_TIMESTAMP,

       PRIMARY KEY (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '用户登录身份';

CREATE TABLE `login_attempt` (
     `id`             BIGINT       NOT NULL COMMENT 'ID',
     `user_id`        BIGINT       DEFAULT NULL COMMENT '用户 ID（失败时可能为空）',
     `login_name`     VARCHAR(64)  NOT NULL COMMENT '登录名',
     `credential_type` VARCHAR(16) NOT NULL COMMENT '凭证类型：PASSWORD / SMS / EMAIL / OAUTH',
     `result`         VARCHAR(16)  NOT NULL COMMENT '结果：SUCCESS / FAIL',
     `reason`         VARCHAR(128) DEFAULT NULL COMMENT '失败原因',
     `ip`             VARCHAR(64)  DEFAULT NULL,
     `user_agent`     VARCHAR(256) DEFAULT NULL,
     `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
     PRIMARY KEY (`id`),
     KEY `idx_login_name` (`login_name`),
     KEY `idx_user_id` (`user_id`),
     KEY `idx_created_at` (`created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT='登录尝试记录';
CREATE TABLE `session` (
   `id`                  BIGINT       NOT NULL COMMENT '会话 ID',
   `user_id`             BIGINT       NOT NULL COMMENT '用户 ID',
   `access_token`        VARCHAR(512) NOT NULL COMMENT '短期访问令牌',
   `refresh_token`       VARCHAR(512) NOT NULL COMMENT '长期刷新令牌',
   `device_info`         VARCHAR(256) DEFAULT NULL COMMENT '设备信息',
   `device_id`           VARCHAR(128) DEFAULT NULL COMMENT '设备指纹',
   `ip`                  VARCHAR(64)  DEFAULT NULL COMMENT '登录 IP',
   `status`              VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE'
       COMMENT '状态：ACTIVE / EXPIRED / REVOKED',
   `access_expires_at`   DATETIME     NOT NULL COMMENT 'Access Token 过期时间',
   `refresh_expires_at`  DATETIME     NOT NULL COMMENT 'Refresh Token 过期时间',
   `last_refresh_at`     DATETIME     DEFAULT NULL COMMENT '最后一次刷新时间',
   `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
   `updated_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
   PRIMARY KEY (`id`),
   UNIQUE KEY `uk_access_token` (`access_token`),
   UNIQUE KEY `uk_refresh_token` (`refresh_token`),
   KEY `idx_user_id` (`user_id`),
   KEY `idx_status_expires` (`status`, `access_expires_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT='会话';
CREATE TABLE `session_refresh_log` (
   `id`            BIGINT       NOT NULL,
   `session_id`    BIGINT       NOT NULL COMMENT '会话 ID',
   `user_id`       BIGINT       NOT NULL,
   `old_token`     VARCHAR(512) DEFAULT NULL,
   `new_token`     VARCHAR(512) NOT NULL,
   `ip`            VARCHAR(64)  DEFAULT NULL,
   `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
   PRIMARY KEY (`id`),
   KEY `idx_session_id` (`session_id`),
   KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会话刷新日志';
CREATE TABLE `mfa_channel` (
   `id`            BIGINT       NOT NULL COMMENT '渠道 ID',
   `user_id`       BIGINT       NOT NULL COMMENT '用户 ID',
   `type`          VARCHAR(16)  NOT NULL COMMENT '类型：EMAIL / SMS / TOTP',
   `address`       VARCHAR(128) DEFAULT NULL COMMENT '地址（邮箱/手机号）',
   `secret`        VARCHAR(256) DEFAULT NULL COMMENT 'TOTP 密钥（加密）',
   `verified`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已验证',
   `enabled`       TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用',
   `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
   `last_used_at`  DATETIME     DEFAULT NULL,
   PRIMARY KEY (`id`),
   UNIQUE KEY `uk_user_type_address` (`user_id`, `type`, `address`),
   KEY `idx_user_id` (`user_id`),
   KEY `idx_address` (`address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MFA 渠道';