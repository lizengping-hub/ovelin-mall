USE auth_database;

DROP PROCEDURE IF EXISTS create_shard_tables;

DELIMITER //

CREATE PROCEDURE create_shard_tables()
BEGIN
    DECLARE shard_id INT DEFAULT 0;
    WHILE shard_id < 2 DO
        SET @statement_text = CONCAT(
            'CREATE TABLE IF NOT EXISTS auth_database.user_', shard_id, ' (',
                '`id`                 BIGINT UNSIGNED NOT NULL COMMENT ''主键ID'',',
                '`display_name`       VARCHAR(50) NOT NULL COMMENT ''用户昵称/显示名，仅展示'',',
                '`avatar_url`         VARCHAR(500) NULL COMMENT ''头像URL'',',
                '`gender`             TINYINT      NULL DEFAULT 0 COMMENT ''性别：0-未知，1-男，2-女'',',
                '`birth_year`         SMALLINT UNSIGNED         NULL COMMENT ''出生年份'',',
                '`birth_month`        TINYINT UNSIGNED         NULL COMMENT ''出生月份'',',
                '`birth_day`          TINYINT UNSIGNED         NULL COMMENT ''出生日期'',',
                '`status`             TINYINT      NOT NULL DEFAULT 1 COMMENT ''账号状态：1-正常，0-禁用，2-注销'',',
                '`register_source`    VARCHAR(20)  NULL COMMENT ''注册来源：如 h5、app、wechat_mini 等'',',
                '`deregistered_at`    TIMESTAMP(3)     NULL DEFAULT NULL COMMENT ''软删除时间，只做记录，状态由status过滤，管理'',',
                ' version             BIGINT           NOT NULL COMMENT ''版本控制'',',
                '`created_at`         TIMESTAMP(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT ''创建时间'',',
                '`updated_at`         TIMESTAMP(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT ''更新时间'',',
                'PRIMARY KEY (id)',
                ') ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci'
        );
        PREPARE create_table FROM @statement_text;
        EXECUTE create_table;
        DEALLOCATE PREPARE create_table;
        SET @statement_text = CONCAT(
            'CREATE TABLE IF NOT EXISTS auth_database.user_identity_', shard_id, ' (',
                'id BIGINT NOT NULL COMMENT ''身份记录ID'',',
                'user_id BIGINT NOT NULL COMMENT ''用户ID'',',
                'identity_type VARCHAR(20) NOT NULL COMMENT ''username / phone / email'',',
                'identifier VARCHAR(255) NOT NULL COMMENT ''标准化登录标识'',',
                'verified_at TIMESTAMP(3) NULL,',
                'last_login_at TIMESTAMP(3) NULL,',
                'created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),',
                'updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                        ON UPDATE CURRENT_TIMESTAMP(3),',
                'PRIMARY KEY (id),',
                'UNIQUE KEY uk_identity_type_and_identifier (identity_type, identifier),',
                'KEY idx_user_id (user_id)',
                ') ENGINE = InnoDB
                      DEFAULT CHARSET = utf8mb4
                      COLLATE = utf8mb4_0900_ai_ci
                      COMMENT = ''用户登录身份'''
        );
        PREPARE create_table FROM @statement_text;
        EXECUTE create_table;
        DEALLOCATE PREPARE create_table;

        SET @statement_text = CONCAT(
                    'CREATE TABLE IF NOT EXISTS auth_database.identity_lookup_', shard_id, ' (',
                        'identity_type VARCHAR(20) NOT NULL COMMENT ''username / phone / email'',',
                        'identifier VARCHAR(255) NOT NULL COMMENT ''标准化登录标识'',',
                        'user_id BIGINT NOT NULL COMMENT ''用户ID'',',
                        'status TINYINT NOT NULL COMMENT ''0 reserved;1 active; 2 releasing'','
                        'created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),',
                        'updated_at  TIMESTAMP(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT ''更新时间'',',
                        'PRIMARY KEY (identity_type, identifier)',
                        ') ENGINE = InnoDB
                              DEFAULT CHARSET = utf8mb4
                              COLLATE = utf8mb4_0900_ai_ci
                              COMMENT = ''登录标识到用户的映射'''
                );
        PREPARE create_table FROM @statement_text;
        EXECUTE create_table;
        DEALLOCATE PREPARE create_table;

        SET @statement_text = CONCAT(
                    'CREATE TABLE IF NOT EXISTS auth_database.user_password_', shard_id, ' (',
                        'user_id BIGINT NOT NULL COMMENT ''用户ID'',',
                        'password_hash VARCHAR(255) NOT NULL COMMENT ''密码哈希'',',
                        'password_changed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),',
                        'failed_attempts INT NOT NULL DEFAULT 0,',
                        'locked_until DATETIME(3) NULL,',
                        'created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),',
                        'updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                                ON UPDATE CURRENT_TIMESTAMP(3),',
                        'PRIMARY KEY (user_id)',
                        ') ENGINE = InnoDB
                              DEFAULT CHARSET = utf8mb4
                              COLLATE = utf8mb4_0900_ai_ci
                              COMMENT = ''用户密码'''
                );
        PREPARE create_table FROM @statement_text;
        EXECUTE create_table;
        DEALLOCATE PREPARE create_table;

        SET @statement_text = CONCAT(
                    'CREATE TABLE IF NOT EXISTS auth_database.user_mfa_factor_', shard_id, ' (',
                        'id BIGINT NOT NULL COMMENT ''二次验证因子ID'',',
                        'user_id BIGINT NOT NULL COMMENT ''用户ID'',',
                        'factor_type VARCHAR(20) NOT NULL COMMENT ''sms / email / totp / webauthn / recovery_code'',',
                        'identity_id BIGINT NULL COMMENT ''短信/邮箱因子引用的 user_identity.id'',',
                        'secret_encrypted VARCHAR(512) NULL COMMENT ''加密后的密钥或凭证，sms/email 为空'',',
                        'status TINYINT NOT NULL DEFAULT 0 COMMENT ''0 待验证 / 1 启用 / 2 停用'',',
                        'verified_at TIMESTAMP(3) NULL,',
                        'last_used_at TIMESTAMP(3) NULL,',
                        'created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),',
                        'updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                                ON UPDATE CURRENT_TIMESTAMP(3),',
                        'PRIMARY KEY (id),',
                        'KEY idx_user_id (user_id)',
                        ') ENGINE = InnoDB
                              DEFAULT CHARSET = utf8mb4
                              COLLATE = utf8mb4_0900_ai_ci
                              COMMENT = ''用户二次验证因子'''
                );
        PREPARE create_table FROM @statement_text;
        EXECUTE create_table;
        DEALLOCATE PREPARE create_table;

SET shard_id = shard_id + 1;
END WHILE;
END//

CALL create_shard_tables()//
DROP PROCEDURE create_shard_tables//

    DELIMITER ;