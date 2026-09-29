
CREATE USER IF NOT EXISTS 'ovelin'@'%' IDENTIFIED BY 'ovelin';
GRANT ALL PRIVILEGES ON shared_database.* TO 'ovelin'@'%';
GRANT ALL PRIVILEGES ON user_database.* TO 'ovelin'@'%';
GRANT ALL PRIVILEGES ON login_database.* TO 'ovelin'@'%';

FLUSH PRIVILEGES;