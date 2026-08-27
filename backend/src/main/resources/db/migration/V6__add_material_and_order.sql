-- Material / Recipe / Order flow change, requested by the user 2026-08-27.
--
-- Three things happen here:
--   1. Materials become first-class records instead of free text repeated on
--      every recipe row (recipe_material duplicated material_name + unit).
--   2. A recipe's batch quantity becomes DERIVED from its materials rather
--      than typed in by hand, so it can't drift from the actual mix.
--   3. Orders (sales orders) are introduced: Client + Site + Recipe + a
--      quantity in m3.

-- ---------------------------------------------------------------------------
-- 1. Material
-- ---------------------------------------------------------------------------
-- density_kg_per_m3 exists because the recipe total has to be expressed in m3
-- while materials are measured in KG or LITRE. LITRE -> m3 is exact
-- (1 m3 = 1000 L), but KG -> m3 needs the material's density and nothing in
-- the schema carried it. It is user-entered per material rather than a
-- hardcoded table of assumed densities (explicitly ruled out by the user).
-- Nullable because LITRE materials don't need it, and because the rows
-- backfilled from existing recipe data below have no density known yet - the
-- application requires it for KG materials on create/update instead.
CREATE TABLE material (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    name               VARCHAR(100)   NOT NULL,
    unit               VARCHAR(20)    NOT NULL,
    density_kg_per_m3  DECIMAL(10,3)  NULL,
    created_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_material_name UNIQUE (name)
) ENGINE=InnoDB;

-- Seed from whatever material names existing recipes already reference, so no
-- recipe loses its materials. Units in the old free-text column were things
-- like 'kg' / 'L'; map them onto the new enum values.
INSERT INTO material (name, unit)
SELECT DISTINCT
       rm.material_name,
       CASE
           WHEN UPPER(TRIM(rm.unit)) IN ('L', 'LITRE', 'LITER', 'LITRES', 'LITERS') THEN 'LITRE'
           ELSE 'KG'
       END
FROM recipe_material rm;

-- ---------------------------------------------------------------------------
-- 2. recipe_material now references material instead of duplicating it
-- ---------------------------------------------------------------------------
ALTER TABLE recipe_material ADD COLUMN material_id BIGINT NULL AFTER recipe_id;

UPDATE recipe_material rm
JOIN material m ON m.name = rm.material_name
SET rm.material_id = m.id;

ALTER TABLE recipe_material MODIFY COLUMN material_id BIGINT NOT NULL;

ALTER TABLE recipe_material
    ADD CONSTRAINT fk_recipe_material_material
    FOREIGN KEY (material_id) REFERENCES material (id);

CREATE INDEX idx_recipe_material_material_id ON recipe_material (material_id);

-- The name/unit now live on material; keeping copies here would let them drift.
ALTER TABLE recipe_material DROP COLUMN material_name;
ALTER TABLE recipe_material DROP COLUMN unit;

-- ---------------------------------------------------------------------------
-- 3. recipe.batch_size -> recipe.total_batch_quantity_m3 (computed)
-- ---------------------------------------------------------------------------
-- Same value, clearer name, and now derived from the material list rather than
-- typed in. Widened from DECIMAL(6,2): a real mix totals well under 1 m3 once
-- converted (e.g. ~0.207 m3), which 2 decimal places would round away.
-- Existing rows keep the batch size they already had.
ALTER TABLE recipe
    CHANGE COLUMN batch_size total_batch_quantity_m3 DECIMAL(12,4) NOT NULL;

-- ---------------------------------------------------------------------------
-- 4. Orders
-- ---------------------------------------------------------------------------
-- Named sales_order, not order: ORDER is a SQL reserved word.
-- Status is UNFULFILLED only for now - the rest of the order lifecycle is
-- deliberately not built yet (user scope decision 2026-08-27).
CREATE TABLE sales_order (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id    BIGINT         NOT NULL,
    site_id      BIGINT         NOT NULL,
    recipe_id    BIGINT         NOT NULL,
    quantity_m3  DECIMAL(12,4)  NOT NULL,
    status       VARCHAR(20)    NOT NULL,
    created_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sales_order_client FOREIGN KEY (client_id) REFERENCES client (id),
    CONSTRAINT fk_sales_order_site   FOREIGN KEY (site_id)   REFERENCES site (id),
    CONSTRAINT fk_sales_order_recipe FOREIGN KEY (recipe_id) REFERENCES recipe (id)
) ENGINE=InnoDB;

CREATE INDEX idx_sales_order_client_id ON sales_order (client_id);
CREATE INDEX idx_sales_order_site_id   ON sales_order (site_id);
CREATE INDEX idx_sales_order_recipe_id ON sales_order (recipe_id);
