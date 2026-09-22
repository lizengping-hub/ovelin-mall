CREATE DATABASE IF NOT EXISTS shared_database
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'id_generator_app'@'%' IDENTIFIED BY 'id_generator_app';
GRANT ALL PRIVILEGES ON shared_database.* TO 'id_generator_app'@'%';

FLUSH PRIVILEGES;

USE shared_database;

CREATE TABLE IF NOT EXISTS id_sequence (
    sequence_name VARCHAR(128) NOT NULL,
    next_value BIGINT UNSIGNED NOT NULL DEFAULT 0,
    allocation_size INT UNSIGNED NOT NULL DEFAULT 1000,
    version BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (sequence_name)
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_0900_ai_ci;



