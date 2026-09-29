USE login_database;

DELIMITER //

CREATE PROCEDURE create_shard_tables()
BEGIN
    DECLARE shard_id INT DEFAULT 0;
    DECLARE suffix CHAR(4);
    WHILE shard_id < 2 DO
        SET @statement_text = CONCAT(
            'CREATE TABLE IF NOT EXISTS login_database.user_identity_', shard_id, ' (',
                'id BIGINT NOT NULL COMMENT ''身份记录ID'',',
                'user_id BIGINT NOT NULL COMMENT ''用户ID'',',
                'identity_type VARCHAR(20) NOT NULL COMMENT ''username / phone / email'',',
                'normalized_identifier VARCHAR(255) NOT NULL COMMENT ''标准化登录标识'',',
                'is_primary TINYINT NOT NULL DEFAULT 0,',
                'verified_at DATETIME NULL,',
                'last_login_at DATETIME NULL,',
                'created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,',
                'updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                        ON UPDATE CURRENT_TIMESTAMP,',
                'PRIMARY KEY (id),',
                'UNIQUE KEY uk_normalized_identifier (normalized_identifier),',
                'KEY idx_user_id (user_id)',
                ') ENGINE = InnoDB
                      DEFAULT CHARSET = utf8mb4
                      COLLATE = utf8mb4_0900_ai_ci
                      COMMENT = ''用户登录身份'''
        );
PREPARE create_login_table FROM @statement_text;
EXECUTE create_login_table;
DEALLOCATE PREPARE create_login_table;

SET shard_id = shard_id + 1;
END WHILE;
END//

CALL create_shard_tables()//
DROP PROCEDURE create_shard_tables//

    DELIMITER ;