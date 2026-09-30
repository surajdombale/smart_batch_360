-- Constrains the status columns to the values their enums actually have.
--
-- They are all plain VARCHAR, so the database accepted anything. That is not a
-- hole the API can be driven through - it validates on the way in - but it did
-- bite during load testing: rows seeded by hand with 'OFF' for the equipment
-- status loaded happily and then made every read of them fail with a 500,
-- because the value could not be mapped back to EquipmentStatus. A constraint
-- turns that into a rejected write at the point of the mistake.
--
-- Checked against the data before writing this: every existing row already
-- satisfies these, so the migration applies without touching anything.
--
-- batch.shift is deliberately left alone - it is free text ("Day", "Night"),
-- not an enum.

ALTER TABLE client
    ADD CONSTRAINT ck_client_status CHECK (status IN ('ACTIVE', 'INACTIVE'));

ALTER TABLE site
    ADD CONSTRAINT ck_site_status CHECK (status IN ('ACTIVE', 'INACTIVE'));

ALTER TABLE driver
    ADD CONSTRAINT ck_driver_status CHECK (status IN ('ACTIVE', 'INACTIVE'));

ALTER TABLE recipe
    ADD CONSTRAINT ck_recipe_status CHECK (status IN ('ACTIVE', 'INACTIVE'));

ALTER TABLE header
    ADD CONSTRAINT ck_header_status CHECK (status IN ('ACTIVE', 'INACTIVE'));

ALTER TABLE vehicle
    ADD CONSTRAINT ck_vehicle_status CHECK (status IN ('AVAILABLE', 'IN_USE', 'MAINTENANCE'));

ALTER TABLE sales_order
    ADD CONSTRAINT ck_sales_order_status
        CHECK (status IN ('UNFULFILLED', 'IN_PROGRESS', 'FULFILLED', 'CANCELLED'));

ALTER TABLE batch
    ADD CONSTRAINT ck_batch_status
        CHECK (status IN ('PENDING', 'IN_PROGRESS', 'PAUSED', 'STOPPED', 'COMPLETED'));

ALTER TABLE batch
    ADD CONSTRAINT ck_batch_mixer_status CHECK (mixer_status IN ('RUNNING', 'STOPPED'));

ALTER TABLE batch
    ADD CONSTRAINT ck_batch_conveyor_status CHECK (conveyor_status IN ('RUNNING', 'STOPPED'));

ALTER TABLE batch
    ADD CONSTRAINT ck_batch_water_valve_status CHECK (water_valve_status IN ('RUNNING', 'STOPPED'));

ALTER TABLE batch
    ADD CONSTRAINT ck_batch_cement_screw_status CHECK (cement_screw_status IN ('RUNNING', 'STOPPED'));

ALTER TABLE batch
    ADD CONSTRAINT ck_batch_compressor_status CHECK (compressor_status IN ('RUNNING', 'STOPPED'));
