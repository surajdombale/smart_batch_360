-- Undoes V15__batch_and_cycle_fields_from_the_reference_plant.sql.
--
-- Run by hand against the smartbatch360 database, with the application closed.
-- Flyway does not run this: it removes the columns V15 added and then forgets
-- that V15 was applied, so an older build validates against the schema again.
-- Anything stored in these columns is lost - take a dump first.

ALTER TABLE batch
    DROP CHECK ck_batch_size_m3,
    DROP CHECK ck_batch_per_cycle_m3;

ALTER TABLE batch_cycle_material
    DROP COLUMN target,
    DROP COLUMN setpoint,
    DROP COLUMN moisture_content,
    DROP COLUMN absorption;

ALTER TABLE batch_cycle
    DROP COLUMN moisture_content,
    DROP COLUMN corrected_water,
    DROP COLUMN corrected_aggregate;

ALTER TABLE batch
    DROP COLUMN batch_size_m3,
    DROP COLUMN per_cycle_m3,
    DROP COLUMN moisture_enabled;

DELETE FROM flyway_schema_history WHERE version = '15';
