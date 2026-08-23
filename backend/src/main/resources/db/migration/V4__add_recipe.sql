-- Recipe Management, requested by the user (2026-08-23) as a prerequisite for
-- Production: the Production mockup's Batch Details panel references a
-- recipe, and there was no recipe table to reference.
-- Fields per the Recipe Management mockup: Recipe ID (implicit PK), Recipe
-- Name, Batch Size, Description, and a Material Proportion list (material
-- name / quantity / unit per row).

CREATE TABLE recipe (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(150)  NOT NULL,
    batch_size    DECIMAL(6,2)  NOT NULL,
    description   VARCHAR(255)  NULL,
    status        VARCHAR(20)   NOT NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE recipe_material (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipe_id      BIGINT        NOT NULL,
    material_name  VARCHAR(100)  NOT NULL,
    quantity       DECIMAL(8,2)  NOT NULL,
    unit           VARCHAR(20)   NOT NULL,
    display_order  INT           NOT NULL DEFAULT 0,
    CONSTRAINT fk_recipe_material_recipe FOREIGN KEY (recipe_id) REFERENCES recipe (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_recipe_material_recipe_id ON recipe_material (recipe_id);
