
CREATE USER IF NOT EXISTS 'ovelin'@'%' IDENTIFIED BY 'ovelin';
GRANT ALL PRIVILEGES ON shared_database.* TO 'ovelin'@'%';
GRANT ALL PRIVILEGES ON auth_database.* TO 'ovelin'@'%';


GRANT ALL PRIVILEGES ON sharding_test.* TO 'ovelin'@'%';
FLUSH PRIVILEGES;