DROP TABLE IF EXISTS t_user;

CREATE TABLE t_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(64)  NOT NULL,
    age         INT,
    status      TINYINT      DEFAULT 1,
    version     INT,
    create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
