USE sharding_test;

DELIMITER //

CREATE PROCEDURE create_shard_tables()
BEGIN
    DECLARE shard_id INT DEFAULT 0;
    WHILE shard_id < 2 DO
        SET @statement_text = CONCAT(
            'CREATE TABLE IF NOT EXISTS sharding_test.user_', shard_id, ' (',
                '`id`                BIGINT UNSIGNED NOT NULL COMMENT ''主键ID'',',
                '`display_name`      VARCHAR(50) NOT NULL COMMENT ''用户昵称/显示名，仅展示'',',
                'PRIMARY KEY (id)',
                ') ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci'
        );
        PREPARE create_table FROM @statement_text;
        EXECUTE create_table;
        DEALLOCATE PREPARE create_table;
        SET @statement_text = CONCAT(
            'CREATE TABLE IF NOT EXISTS sharding_test.user_identity_', shard_id, ' (',
                'id BIGINT NOT NULL COMMENT ''身份记录ID'',',
                'user_id BIGINT NOT NULL COMMENT ''用户ID'',',
                'identity_type VARCHAR(20) NOT NULL COMMENT ''username / phone / email'',',
                'identifier VARCHAR(255) NOT NULL COMMENT ''标准化登录标识'',',
                'PRIMARY KEY (id),',
                'UNIQUE KEY uk_identifier_type_and_identity (identity_type, identifier),',
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
            'CREATE TABLE IF NOT EXISTS sharding_test.user_by_phone_lookup_', shard_id, ' (',
                'user_id BIGINT NOT NULL COMMENT ''用户ID'',',
                'phone VARCHAR(255) NOT NULL COMMENT ''手机号'',',
                'PRIMARY KEY (user_id),',
                'UNIQUE KEY uk_phone (phone)',
                ') ENGINE = InnoDB
                      DEFAULT CHARSET = utf8mb4
                      COLLATE = utf8mb4_0900_ai_ci
                      COMMENT = ''用户登录身份'''
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