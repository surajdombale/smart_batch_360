-- One-time local dev setup. Run this once against your MySQL server (e.g. via
-- MySQL Workbench, or `mysql -u root -p < backend/src/main/resources/db/dev-setup.sql`)
-- as a user with admin rights. It only creates a database + a low-privilege
-- application user scoped to that database - it does not touch anything else.
-- Flyway (V1__init_schema.sql) creates the actual tables on first backend startup.

CREATE DATABASE IF NOT EXISTS smartbatch360 CHARACTER SET utf8mb4;

CREATE USER IF NOT EXISTS 'smartbatch360'@'localhost' IDENTIFIED BY 'smartbatch360';
GRANT ALL PRIVILEGES ON smartbatch360.* TO 'smartbatch360'@'localhost';
FLUSH PRIVILEGES;
