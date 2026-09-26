-- Indexes the column both batch screens sort by.
--
-- Batch Reports sorts newest-first by default, and Production loads the 200 most
-- recent batches the same way. cycle_date_time had no index, so MySQL scanned
-- every batch row and sorted the lot to return 20 - a full scan plus filesort on
-- every open. Measured at 30,000 batches: Production's load went from
-- 0.15-0.78s to 0.03-0.05s, and the first page of Batch Reports from
-- 0.07-0.21s to 0.02-0.05s.
--
-- Additive, and the column is already NOT NULL, so nothing else changes.
-- Deep pages (page 500) do not benefit - a large OFFSET still walks the index -
-- but nobody pages 500 deep; they filter instead.

CREATE INDEX idx_batch_cycle_date_time ON batch (cycle_date_time);
