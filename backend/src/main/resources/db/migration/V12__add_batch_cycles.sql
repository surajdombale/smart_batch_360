-- What each mixer cycle actually weighed, as reported by the PLC.
--
-- The batch report (templates supplied 03-Oct-2026) is a grid: one row per
-- cycle, one column per material, with totals down the cycles and across the
-- materials. Until now a batch carried a single achieved figure per material -
-- the finished total - and no record of how it got there.
--
-- The PLC posts one message per cycle carrying every material, which is how the
-- plant thinks about it. Stored normalised rather than as twenty columns: the
-- report needs to total a material down the cycles, a recipe may use any number
-- of materials up to the twenty the report shows, and a sparse twenty-column
-- row would be mostly empty for a six-material mix.
--
-- cycle_time is when the PLC ran the cycle, which is not when it told us -
-- a retry or a buffered message would otherwise rewrite the plant's own clock.

CREATE TABLE batch_cycle (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id      BIGINT     NOT NULL,
    cycle_number  INT        NOT NULL,
    cycle_time    TIMESTAMP  NOT NULL,
    created_at    TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_batch_cycle_batch FOREIGN KEY (batch_id) REFERENCES batch (id) ON DELETE CASCADE,
    -- One record per cycle of a batch. A PLC that retries a message must correct
    -- the cycle it already sent rather than add a second copy of it.
    CONSTRAINT uq_batch_cycle_number UNIQUE (batch_id, cycle_number),
    CONSTRAINT ck_batch_cycle_number CHECK (cycle_number >= 1)
) ENGINE=InnoDB;

CREATE TABLE batch_cycle_material (
    id              BIGINT        AUTO_INCREMENT PRIMARY KEY,
    batch_cycle_id  BIGINT        NOT NULL,
    material_name   VARCHAR(100)  NOT NULL,
    achieved        DECIMAL(8,2)  NOT NULL,
    display_order   INT           NOT NULL DEFAULT 0,
    CONSTRAINT fk_batch_cycle_material_cycle FOREIGN KEY (batch_cycle_id)
        REFERENCES batch_cycle (id) ON DELETE CASCADE,
    CONSTRAINT uq_batch_cycle_material UNIQUE (batch_cycle_id, material_name)
) ENGINE=InnoDB;

-- The report reads every cycle of one batch, in order.
CREATE INDEX idx_batch_cycle_batch ON batch_cycle (batch_id, cycle_number);
