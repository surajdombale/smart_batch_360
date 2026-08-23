# SmartBatch360 — Database Specification

## Database

MySQL is the required database.

Use:
- Spring Data JPA
- Hibernate
- Flyway

## Important constraint

The supplied documents do not provide complete field-by-field schemas for Customer, Site, Vehicle, Driver or Header.

Therefore Claude must inspect the supplied UI/reference requirements and existing repository before creating final migrations.

Do not manufacture a large schema from generic fleet-management assumptions.

## Phase 1 tables

Target tables:
- client — renamed from "customer" 2026-08-23 at the user's explicit request (V3__rename_customer_to_client.sql); docs/01_REQUIREMENTS.md
- site
- vehicle
- driver
- header — built 2026-08-17; docs/01_REQUIREMENTS.md

Use a consistent naming convention.

## Migration rules

Every schema change must be a Flyway migration.

Never edit an already-applied migration to change production structure. Add a new migration.

## Integrity

Where the actual requirements specify unique identifiers/codes/registration/license values, enforce uniqueness at the database level.

Foreign keys should be added only where the requirements establish the relationship.

## Configuration

MySQL connection information must be externalized.

Do not commit:
- username/password
- production database URL
- private keys
- API secrets

## Future compatibility

The database will eventually support:
- BATCH_DATA
- recipes
- material data MAT1–MAT20
- reporting
- production
- PLC-related data
- analytics
- event history

Do not create those future tables during the restricted CRUD phase unless an existing repository migration already requires them.
