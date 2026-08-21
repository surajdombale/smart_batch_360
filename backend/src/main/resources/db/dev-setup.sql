-- OPTIONAL. The backend's first-run setup wizard (DatabaseBootstrap) now
-- creates the "smartbatch360" database itself when given an admin login -
-- you do not need to run this script for a normal install.
--
-- Use this only if you'd rather pre-create a low-privilege, scoped
-- "smartbatch360" application user yourself and enter *that* into the
-- wizard instead of an admin account. Run it once against your MySQL
-- server (e.g. via MySQL Workbench, or
-- `mysql -u root -p < backend/src/main/resources/db/dev-setup.sql`) as a
-- user with admin rights. It only creates a database + a low-privilege
-- application user scoped to that database - it does not touch anything else.
-- Flyway (V1__init_schema.sql, V2__add_header.sql, ...) creates the actual
-- tables on first backend startup either way.

CREATE DATABASE IF NOT EXISTS smartbatch360 CHARACTER SET utf8mb4;

CREATE USER IF NOT EXISTS 'smartbatch360'@'localhost' IDENTIFIED BY 'smartbatch360';
GRANT ALL PRIVILEGES ON smartbatch360.* TO 'smartbatch360'@'localhost';
FLUSH PRIVILEGES;
