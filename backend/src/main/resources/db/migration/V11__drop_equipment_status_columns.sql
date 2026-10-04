-- Drops the five simulated equipment status columns.
--
-- Requested 2026-10-03 with the new Batch Report templates: mixer, conveyor,
-- water valve, cement screw and compressor status are to go from the database
-- entirely, not just from the report.
--
-- They were manual stand-ins for hardware that was never wired up - a batch
-- carried five dropdowns an operator set by hand, which told nobody anything
-- the plant did not already know. The report that replaces them records what
-- each cycle actually weighed.
--
-- Destructive, unlike every migration before it. One batch exists at the time
-- of writing (batch "1", created 03-Oct-2026, with all five reading RUNNING);
-- those five values are lost and cannot be recovered from the schema. They were
-- set by hand and describe no real hardware, which is why the user asked for
-- them to go. A dump was taken before applying this.
--
-- The CHECK constraints V10 put on these columns go with them.

ALTER TABLE batch
    DROP COLUMN mixer_status,
    DROP COLUMN conveyor_status,
    DROP COLUMN water_valve_status,
    DROP COLUMN cement_screw_status,
    DROP COLUMN compressor_status;
