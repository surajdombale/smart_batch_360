-- Link production batches to the sales order they were produced against,
-- so an order's fulfilment can be measured (produced vs ordered m3) instead
-- of resting entirely on the operator's word. Requested 2026-09-05 as the
-- follow-on to the Order lifecycle.
--
-- Nullable on purpose: batches predate orders entirely, and ad-hoc
-- production that isn't against any order stays legitimate. Existing rows
-- keep order_id NULL and behave exactly as before.
ALTER TABLE batch ADD COLUMN order_id BIGINT NULL AFTER recipe_id;

ALTER TABLE batch
    ADD CONSTRAINT fk_batch_order
    FOREIGN KEY (order_id) REFERENCES sales_order (id);

CREATE INDEX idx_batch_order_id ON batch (order_id);
