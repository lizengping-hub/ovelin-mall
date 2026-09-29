USE user_database;

DELIMITER //

CREATE PROCEDURE create_shard_tables()
BEGIN
    DECLARE shard_id INT DEFAULT 0;
    DECLARE suffix CHAR(4);
    WHILE shard_id < 2 DO

        SET @statement_text = CONCAT(
            'CREATE TABLE IF NOT EXISTS user_database.user_', shard_id, ' (',
                '`id`                BIGINT UNSIGNED NOT NULL COMMENT ''主键ID'',',
                '`username`          VARCHAR(50)  NULL COMMENT ''用户昵称/显示名，仅展示'',',
                '`phone_e164`        VARCHAR(20)  NULL COMMENT ''手机号，E164格式，用于通知，如 +8613800138000'',',
                '`phone_raw_input`   VARCHAR(50)  NULL COMMENT ''手机号原始输入，仅展示用'',',
                '`email`             VARCHAR(254) NULL COMMENT ''邮箱，用于通知，统一存小写'',',
                '`avatar_url`        VARCHAR(500) NULL COMMENT ''头像URL'',',
                '`gender`            TINYINT      NULL DEFAULT 0 COMMENT ''性别：0-未知，1-男，2-女'',',
                '`birthday`          DATE         NULL COMMENT ''生日'',',
                '`status`            TINYINT      NOT NULL DEFAULT 1 COMMENT ''账号状态：1-正常，0-禁用，2-注销'',',
                '`register_source`   VARCHAR(20)  NULL COMMENT ''注册来源：如 h5、app、wechat_mini 等'',',
                '`created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',',
                '`updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',',
                '`deleted_at`        DATETIME     NULL COMMENT ''软删除时间，NULL表示未删除'',',
                'PRIMARY KEY (id)',
                ') ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci'
        );
PREPARE create_user_table FROM @statement_text;
EXECUTE create_user_table;
DEALLOCATE PREPARE create_user_table;

SET shard_id = shard_id + 1;
END WHILE;
END//

CALL create_shard_tables()//
DROP PROCEDURE create_shard_tables//

DELIMITER ;