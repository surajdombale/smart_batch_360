-- Renames "customer" to "client" throughout, per the user's explicit request
-- (2026-08-23) - not just a UI label change. Also adds the new optional
-- "address" field on client, requested at the same time.
-- Never edit V1/V2 in place (docs/04_DATABASE.md) - this is an additive
-- migration that transforms the existing schema instead.

ALTER TABLE site DROP FOREIGN KEY fk_site_customer;

RENAME TABLE customer TO client;

ALTER TABLE client ADD COLUMN address VARCHAR(255) NULL AFTER phone;

ALTER TABLE site RENAME COLUMN customer_id TO client_id;

DROP INDEX idx_site_customer_id ON site;
CREATE INDEX idx_site_client_id ON site (client_id);

ALTER TABLE site ADD CONSTRAINT fk_site_client FOREIGN KEY (client_id) REFERENCES client (id);
