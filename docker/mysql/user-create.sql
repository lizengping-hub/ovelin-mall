CREATE DATABASE IF NOT EXISTS user_database
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'identity_app'@'%' IDENTIFIED BY 'identity_app';
GRANT ALL PRIVILEGES ON user_database.* TO 'identity_app'@'%';
FLUSH PRIVILEGES;

USE user_database;


CREATE TABLE `user` (


                        `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',

    -- 以下均为展示信息 / 通知渠道，不作登录凭证，不加唯一约束
                        `phone_e164`        VARCHAR(20)  NULL COMMENT '手机号，E164格式，用于通知，如 +8613800138000',
                        `phone_raw_input`   VARCHAR(50)  NULL COMMENT '手机号原始输入，仅展示用',

                        `email`             VARCHAR(254) NULL COMMENT '邮箱，用于通知，统一存小写',

                        `username`          VARCHAR(50)  NULL COMMENT '用户昵称/显示名，仅展示',
                        `avatar_url`        VARCHAR(500) NULL COMMENT '头像URL',
                        `gender`            TINYINT      NOT NULL DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
                        `birthday`          DATE         NULL COMMENT '生日',

    -- 状态与审计字段
                        `status`            TINYINT      NOT NULL DEFAULT 1 COMMENT '账号状态：1-正常，0-禁用，2-注销',
                        `register_source`   VARCHAR(20)  NULL COMMENT '注册来源：如 h5、app、wechat_mini 等',

                        `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        `deleted_at`        DATETIME     NULL COMMENT '软删除时间，NULL表示未删除',

                        PRIMARY KEY (`id`),
                        KEY `idx_status` (`status`),
                        KEY `idx_created_at` (`created_at`),
                        KEY `idx_phone_e164` (`phone_e164`),
                        KEY `idx_email` (`email`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '用户表';


DELIMITER //

CREATE PROCEDURE create_identity_shard_tables()
BEGIN
    DECLARE shard_id INT DEFAULT 0;
    DECLARE suffix CHAR(4);
    WHILE shard_id < 256 DO
        SET suffix = LPAD(shard_id, 4, '0');

        SET @statement_text = CONCAT(
            'CREATE TABLE IF NOT EXISTS user_database.user_subject_', suffix, ' (',
            'user_id CHAR(36) NOT NULL,',
            'user_status VARCHAR(16) NOT NULL,',
            'display_name VARCHAR(128) NOT NULL,',
            'password_hash VARCHAR(255) NULL,',
            'login_methods_snapshot JSON NOT NULL,',
            'version BIGINT NOT NULL DEFAULT 0,',
            'last_login_at TIMESTAMP NULL,',
            'created_at TIMESTAMP NOT NULL,',
            'updated_at TIMESTAMP NOT NULL,',
            'PRIMARY KEY (user_id)',
            ') ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci'
        );
PREPARE create_user_table FROM @statement_text;
EXECUTE create_user_table;
DEALLOCATE PREPARE create_user_table;

SET @statement_text = CONCAT(
            'CREATE TABLE IF NOT EXISTS login_database.login_identity_', suffix, ' (',
            'identity_type VARCHAR(16) NOT NULL,',
            'normalized_identifier VARCHAR(512) NOT NULL,',
            'user_id CHAR(36) NOT NULL,',
            'identity_status VARCHAR(16) NOT NULL,',
            'verified_at TIMESTAMP NULL,',
            'last_used_at TIMESTAMP NULL,',
            'version BIGINT NOT NULL DEFAULT 0,',
            'created_at TIMESTAMP NOT NULL,',
            'updated_at TIMESTAMP NOT NULL,',
            'PRIMARY KEY (identity_type, normalized_identifier),',
            'INDEX idx_login_identity_user (user_id, created_at)',
            ') ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci'
        );
PREPARE create_login_table FROM @statement_text;
EXECUTE create_login_table;
DEALLOCATE PREPARE create_login_table;

SET shard_id = shard_id + 1;
END WHILE;
END//

CALL create_identity_shard_tables()//
DROP PROCEDURE create_identity_shard_tables//

    DELIMITER ;