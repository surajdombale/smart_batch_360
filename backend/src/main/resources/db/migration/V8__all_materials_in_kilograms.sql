-- Everything is measured in kilograms.
--
-- Requested 2026-09-07. Materials previously carried their own unit (KG or
-- LITRE) plus a density, and a recipe's batch size was derived by converting
-- every line to cubic metres. That conversion was the problem: a KG material
-- could not be used at all until someone supplied a density, the densities
-- had to be maintained per material, and orders and batches were then sized
-- in m3 against recipes whose ingredients are weighed in kg on the plant
-- floor. Dropping the conversion removes the whole class of problem - a
-- recipe's batch size is now simply the sum of what goes into it.

ALTER TABLE material DROP COLUMN unit;
ALTER TABLE material DROP COLUMN density_kg_per_m3;

ALTER TABLE recipe RENAME COLUMN total_batch_quantity_m3 TO total_batch_quantity_kg;

-- Recompute rather than convert: the total is derived from the lines, so the
-- lines are the truth. (The old m3 figures were not convertible back without
-- the densities this migration removes.)
UPDATE recipe r
SET r.total_batch_quantity_kg = COALESCE(
        (SELECT SUM(rm.quantity) FROM recipe_material rm WHERE rm.recipe_id = r.id), 0);

-- Order quantities keep their numbers; only the unit they are read in changes.
-- Existing rows were entered as m3 and will need reviewing by hand - there is
-- no honest conversion available once the densities are gone.
ALTER TABLE sales_order RENAME COLUMN quantity_m3 TO quantity_kg;
