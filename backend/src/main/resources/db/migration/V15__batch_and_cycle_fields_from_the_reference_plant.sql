-- The fields the reference plant's BATCHTABLE and CYCLEDATA carry that a batch
-- and its cycles here did not.
--
-- Requested 06-Oct-2026: the batch report (sample "101.pdf") is printed from
-- those two tables, so everything they hold has to have a home here. Compared
-- column by column, most of it already did - under a foreign key rather than a
-- repeated name:
--
--   CUSTOMERNAME, CUSTOMERADD      batch.client_id  -> client
--   SITENAME, SITEADD              batch.site_id    -> site
--   DRIVERNAME, VEHICLENUMBER      batch.driver_id, batch.vehicle_id
--   RECIPEID, RECIPENAME           batch.recipe_id  -> recipe
--   ORDERID, ORDERQUANTITY         batch.order_id   -> sales_order
--   BATCHNUMBER, SHIFT NUMBER      batch.batch_number, batch.shift
--   CYCLEDATE, CYCLETIME           batch_cycle.cycle_time (one instant, not two columns)
--   CYCLENUMBER                    batch_cycle.cycle_number
--   AGG1..ADMIXNEW NAME / ACHI     batch_cycle_material, one row per material
--   TARGETTOTAL, SETTOTAL, TOTAL   summed from those rows, never stored
--
-- The reference keeps fourteen fixed material slots as fifty-six columns and
-- repeats every name on every cycle; its own data shows where that leads (a
-- batch and its cycles naming different recipes). The structure here stays one
-- row per material, as V12 decided. What this adds is the remainder.
--
-- All nullable or defaulted: one batch exists, made before any of this was
-- recorded, and nothing is assumed on its behalf.

-- BATCHQUANTITY and PERCYCLEQUANTITY. A load is sized in m3 and split into
-- cycles; until now only the resulting kilograms were kept, so the size the
-- operator asked for had to be worked back through settings that can change.
-- MOISTUREENABLE is the plant's switch for moisture correction on this load.
ALTER TABLE batch
    ADD COLUMN batch_size_m3     DECIMAL(8,4)  NULL AFTER driver_id,
    ADD COLUMN per_cycle_m3      DECIMAL(8,4)  NULL AFTER batch_size_m3,
    ADD COLUMN moisture_enabled  BOOLEAN       NOT NULL DEFAULT FALSE AFTER shift;

-- MOISTURECONTENT, CORRECTEDWATER, CORRECTEDAGGREGATE: what the PLC measured
-- and corrected for the cycle as a whole. Stored as reported - nothing here
-- calculates a correction.
ALTER TABLE batch_cycle
    ADD COLUMN moisture_content     DECIMAL(6,2)  NULL AFTER cycle_time,
    ADD COLUMN corrected_water      DECIMAL(8,2)  NULL AFTER moisture_content,
    ADD COLUMN corrected_aggregate  DECIMAL(8,2)  NULL AFTER corrected_water;

-- ...TRG and ...STP per material per cycle, and the per-aggregate
-- MOISTURECONTENTAGGn / ABSORPTIONAGGn. A cycle used to record only what was
-- weighed; the report prints what it was aiming at beside it.
ALTER TABLE batch_cycle_material
    ADD COLUMN target            DECIMAL(8,2)  NULL AFTER material_name,
    ADD COLUMN setpoint          DECIMAL(8,2)  NULL AFTER target,
    ADD COLUMN moisture_content  DECIMAL(6,2)  NULL AFTER achieved,
    ADD COLUMN absorption        DECIMAL(6,2)  NULL AFTER moisture_content;

ALTER TABLE batch
    ADD CONSTRAINT ck_batch_size_m3 CHECK (batch_size_m3 IS NULL OR batch_size_m3 > 0),
    ADD CONSTRAINT ck_batch_per_cycle_m3 CHECK (per_cycle_m3 IS NULL OR per_cycle_m3 > 0);
