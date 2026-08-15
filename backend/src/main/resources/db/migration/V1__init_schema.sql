-- SmartBatch360 Phase 1 schema.
-- Tables: customer, site, vehicle, driver.
-- Fields are limited to what is explicitly confirmed by the supplied UI
-- reference (list-view columns) - see docs/01_REQUIREMENTS.md and
-- docs/02_UI_REFERENCE.md. No Header table: undefined by the source
-- documents and intentionally excluded from Phase 1.

CREATE TABLE customer (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(150)  NOT NULL,
    contact_person VARCHAR(150)  NOT NULL,
    phone          VARCHAR(20)   NOT NULL,
    status         VARCHAR(20)   NOT NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE site (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    customer_id BIGINT       NOT NULL,
    location    VARCHAR(150) NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_site_customer FOREIGN KEY (customer_id) REFERENCES customer (id)
) ENGINE=InnoDB;

CREATE INDEX idx_site_customer_id ON site (customer_id);

CREATE TABLE driver (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    phone       VARCHAR(20)  NOT NULL,
    license_no  VARCHAR(50)  NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_driver_license_no UNIQUE (license_no)
) ENGINE=InnoDB;

CREATE TABLE vehicle (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_number         VARCHAR(30)    NOT NULL,
    driver_id              BIGINT         NULL,
    capacity_cubic_meters  DECIMAL(6,2)   NOT NULL,
    status                 VARCHAR(20)    NOT NULL,
    created_at             TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_vehicle_number UNIQUE (vehicle_number),
    CONSTRAINT fk_vehicle_driver FOREIGN KEY (driver_id) REFERENCES driver (id)
) ENGINE=InnoDB;

CREATE INDEX idx_vehicle_driver_id ON vehicle (driver_id);
