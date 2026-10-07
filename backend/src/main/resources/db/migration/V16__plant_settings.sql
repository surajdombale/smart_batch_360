-- Plant settings move out of Company Details into their own table.
--
-- Requested 07-Oct-2026: Company Details is for the company, and the plant's
-- own figures belong under Settings > Plant Details. Supervisor name and mixer
-- capacity move across; plant capacity is new.
--
-- Mix time and discharge time are dropped rather than moved - the user's list
-- for Plant Details does not include them. The batch report templates print
-- both, so they will need a home again; the likely one is the batch itself,
-- reported by the PLC alongside the cycles, since they describe a run rather
-- than a setting. Raised rather than guessed.
--
-- One row, enforced: there is one plant. A second row would make "the mixer
-- capacity" a question with two answers, which is how the old header table
-- ended up being searched for whichever row happened to have a value.

CREATE TABLE plant_settings (
    id                          BIGINT        NOT NULL PRIMARY KEY,
    supervisor_name             VARCHAR(150)  NULL,
    mixer_capacity_m3           DECIMAL(4,2)  NULL,
    -- What the plant can turn out in an hour, in cubic metres.
    plant_capacity_m3_per_hour  DECIMAL(6,2)  NULL,
    created_at                  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT ck_plant_settings_single_row CHECK (id = 1),
    CONSTRAINT ck_plant_settings_mixer_capacity CHECK (mixer_capacity_m3 IS NULL
        OR (mixer_capacity_m3 >= 0.1 AND mixer_capacity_m3 <= 10)),
    CONSTRAINT ck_plant_settings_plant_capacity CHECK (plant_capacity_m3_per_hour IS NULL
        OR plant_capacity_m3_per_hour > 0)
) ENGINE=InnoDB;

-- Carry across whatever the company row already holds, so a plant that set its
-- mixer capacity yesterday does not have to set it again.
INSERT INTO plant_settings (id, supervisor_name, mixer_capacity_m3)
SELECT 1,
       (SELECT supervisor_name FROM header WHERE supervisor_name IS NOT NULL ORDER BY id LIMIT 1),
       (SELECT mixer_capacity_m3 FROM header WHERE mixer_capacity_m3 IS NOT NULL ORDER BY id LIMIT 1);

ALTER TABLE header
    DROP CONSTRAINT ck_header_mix_time,
    DROP CONSTRAINT ck_header_discharge_time,
    DROP CONSTRAINT ck_header_mixer_capacity;

ALTER TABLE header
    DROP COLUMN supervisor_name,
    DROP COLUMN mix_time_seconds,
    DROP COLUMN discharge_time_seconds,
    DROP COLUMN mixer_capacity_m3;
