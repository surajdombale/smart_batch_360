-- Mixer capacity becomes an editable plant setting.
--
-- It was configuration only (smartbatch360.plant.mixer-capacity-m3), which
-- meant changing it needed a file edit and a restart. The user asked on
-- 06-Oct-2026 to keep it editable, and it sits with the other plant settings
-- the batch report needs - mix time and discharge time - on Company Details.
--
-- Nullable, like every field added in V13: a company row already exists. With
-- no value here the configured property is used, and with neither, production
-- planning says so rather than guessing a capacity.

ALTER TABLE header
    ADD COLUMN mixer_capacity_m3 DECIMAL(4,2) NULL AFTER discharge_time_seconds;

-- The plant's own range for a mixer, the same one the setting has always been
-- validated against.
ALTER TABLE header
    ADD CONSTRAINT ck_header_mixer_capacity CHECK (mixer_capacity_m3 IS NULL
        OR (mixer_capacity_m3 >= 0.1 AND mixer_capacity_m3 <= 10));
