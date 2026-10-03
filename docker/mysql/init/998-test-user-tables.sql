USE sharding_test_user_database;

DELIMITER //

CREATE PROCEDURE create_shard_tables()
BEGIN
    DECLARE shard_id INT DEFAULT 0;
    DECLARE suffix CHAR(4);
    WHILE shard_id < 2 DO

        SET @statement_text = CONCAT(
            'CREATE TABLE IF NOT EXISTS sharding_test_user_database.sharding_test_user_', shard_id, ' (',
                '`id`                BIGINT UNSIGNED NOT NULL COMMENT ''主键ID'',',
                '`display_name`      VARCHAR(50) NOT NULL COMMENT ''用户昵称/显示名，仅展示'',',
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