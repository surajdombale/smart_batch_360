-- Production, requested by the user (2026-08-23) as the next Phase 2 module.
-- Scope confirmed with the user: real Batch CRUD (not just a UI mock),
-- referencing Client/Site/Vehicle/Driver/Recipe, with manual/simulated
-- equipment status and batch controls (no PLC integration - still
-- intentionally postponed per docs/06_SCOPE_AND_ROADMAP.md).
-- Fields per docs/02_UI_REFERENCE.md's Production reference and the
-- BATCH_DATA description in docs/06_SCOPE_AND_ROADMAP.md, simplified to a
-- flexible material list (same pattern as recipe_material) rather than
-- fixed MAT1-MAT20 columns.

CREATE TABLE batch (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_number         VARCHAR(30)   NOT NULL,
    recipe_id            BIGINT        NOT NULL,
    client_id            BIGINT        NOT NULL,
    site_id              BIGINT        NOT NULL,
    vehicle_id           BIGINT        NOT NULL,
    driver_id            BIGINT        NOT NULL,
    target_quantity      DECIMAL(8,2)  NOT NULL,
    produced_quantity    DECIMAL(8,2)  NOT NULL DEFAULT 0,
    cycle_date_time       TIMESTAMP     NOT NULL,
    cycle_number         INT           NULL,
    shift                VARCHAR(50)   NULL,
    status               VARCHAR(20)   NOT NULL,
    mixer_status         VARCHAR(20)   NOT NULL DEFAULT 'STOPPED',
    conveyor_status      VARCHAR(20)   NOT NULL DEFAULT 'STOPPED',
    water_valve_status   VARCHAR(20)   NOT NULL DEFAULT 'STOPPED',
    cement_screw_status  VARCHAR(20)   NOT NULL DEFAULT 'STOPPED',
    compressor_status    VARCHAR(20)   NOT NULL DEFAULT 'STOPPED',
    created_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_batch_number UNIQUE (batch_number),
    CONSTRAINT fk_batch_recipe FOREIGN KEY (recipe_id) REFERENCES recipe (id),
    CONSTRAINT fk_batch_client FOREIGN KEY (client_id) REFERENCES client (id),
    CONSTRAINT fk_batch_site FOREIGN KEY (site_id) REFERENCES site (id),
    CONSTRAINT fk_batch_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle (id),
    CONSTRAINT fk_batch_driver FOREIGN KEY (driver_id) REFERENCES driver (id)
) ENGINE=InnoDB;

CREATE INDEX idx_batch_recipe_id ON batch (recipe_id);
CREATE INDEX idx_batch_client_id ON batch (client_id);
CREATE INDEX idx_batch_site_id ON batch (site_id);
CREATE INDEX idx_batch_vehicle_id ON batch (vehicle_id);
CREATE INDEX idx_batch_driver_id ON batch (driver_id);

CREATE TABLE batch_material (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id       BIGINT        NOT NULL,
    material_name  VARCHAR(100)  NOT NULL,
    target         DECIMAL(8,2)  NOT NULL,
    setpoint       DECIMAL(8,2)  NOT NULL,
    achieved       DECIMAL(8,2)  NOT NULL,
    unit           VARCHAR(20)   NOT NULL,
    display_order  INT           NOT NULL DEFAULT 0,
    CONSTRAINT fk_batch_material_batch FOREIGN KEY (batch_id) REFERENCES batch (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_batch_material_batch_id ON batch_material (batch_id);
