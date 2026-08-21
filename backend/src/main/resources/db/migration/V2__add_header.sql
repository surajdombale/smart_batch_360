-- Header module, defined per user clarification (2026-08-17): the company/plant
-- letterhead block that will appear at the top of printed Batch Logs and
-- Order/Recipe reports once those (out-of-scope) modules exist. Header itself
-- has no dependency on Batch/Recipe data - it is a standalone branding record.
-- Multiple named headers are supported (e.g. one per plant/branch).

CREATE TABLE header (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_name  VARCHAR(150) NOT NULL,
    plant_name    VARCHAR(150) NOT NULL,
    address       VARCHAR(255) NULL,
    phone         VARCHAR(20)  NULL,
    email         VARCHAR(150) NULL,
    gstin         VARCHAR(20)  NULL,
    status        VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;
