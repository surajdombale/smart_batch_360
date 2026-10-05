-- The fields the batch report's letterhead and header block need.
--
-- From the templates supplied 03-Oct-2026 and the user's answer on 04-Oct that
-- these belong on Company Details rather than a Settings module:
--
--   City, Pin code   - the letterhead's address block
--   Company logo     - printed at the top of the report
--   Supervisor name  - named on every batch report
--   Mix time         - how long the mixer runs
--   Discharge time   - how long discharging takes
--
-- All nullable. A letterhead is filled in incrementally - the existing rule for
-- address, phone, email and GSTIN - and a company row already exists, so
-- requiring them would make it invalid the moment this ran.
--
-- The logo is stored here rather than as a path on disk: this app is delivered
-- as a self-contained install, and a path would point at a file the next PC
-- does not have. MEDIUMBLOB holds up to 16MB, far beyond any letterhead.

ALTER TABLE header
    ADD COLUMN city                    VARCHAR(100)  NULL AFTER address,
    ADD COLUMN pin_code                VARCHAR(10)   NULL AFTER city,
    ADD COLUMN supervisor_name         VARCHAR(150)  NULL AFTER gstin,
    ADD COLUMN mix_time_seconds        INT           NULL AFTER supervisor_name,
    ADD COLUMN discharge_time_seconds  INT           NULL AFTER mix_time_seconds,
    ADD COLUMN logo                    MEDIUMBLOB    NULL AFTER discharge_time_seconds,
    ADD COLUMN logo_content_type       VARCHAR(100)  NULL AFTER logo;

-- Times are durations in seconds, not clock times. An hour is already far
-- longer than either step takes.
ALTER TABLE header
    ADD CONSTRAINT ck_header_mix_time CHECK (mix_time_seconds IS NULL
        OR (mix_time_seconds >= 0 AND mix_time_seconds <= 3600)),
    ADD CONSTRAINT ck_header_discharge_time CHECK (discharge_time_seconds IS NULL
        OR (discharge_time_seconds >= 0 AND discharge_time_seconds <= 3600));
