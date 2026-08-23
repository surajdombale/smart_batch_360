# SmartBatch360 — UI Reference Specification

## Source document status

The supplied UI document states that its screenshots are conceptual presentation mockups generated from the agreed SmartBatch360 module structure. They illustrate intended content and layout and are not exact screenshots of the current running application.

Therefore:
- use the document as the visual/layout reference
- do not assume every visual element represents an implemented backend feature
- do not invent hidden business rules from a screenshot

## Application visual direction

The UI represents an industrial batching plant management system.

The application is intended for:
- operators
- supervisors
- management

The hierarchy should make operational information easy to scan.

## Dashboard reference

The UI reference describes:
- KPI cards
- production trend
- recent batch activity
- backend/database/API/PLC status
- active recipe
- last batch
- average batch time
- automatic refresh

The dashboard should remain visually aligned with the reference while respecting the current Phase 1 scope.

## Production reference

The conceptual Production screen contains:
- batch details
- recipe
- customer
- site
- vehicle
- driver
- target
- produced
- remaining quantity
- large progress indicator
- equipment status
- material target/setpoint/achieved values
- future production controls

Do not implement Production in the current phase.

## Batch Reports reference

The conceptual reports screen contains:
- batch number/range
- date range
- customer
- site
- vehicle
- driver
- recipe
- pagination
- sorting
- batch detail
- PDF/Excel/print actions

Do not implement in the current phase.

## Material Consumption reference

The conceptual module contains:
- daily/weekly/monthly consumption
- material target vs actual
- variance/wastage
- charts
- material data based on MAT1–MAT20 fields

Do not implement in the current phase.

## Recipe Management reference

The conceptual module contains:
- recipe list/details
- Recipe ID
- batch size
- grade/description
- material proportions
- quantities/units
- create/edit/copy/delete
- future versioning/approval history

**Built 2026-08-23**, at the user's explicit request, as a prerequisite for
Production (which references a recipe): Recipe Name, Batch Size,
Description, Status, and an editable Material Proportion list
(material name/quantity/unit per row, at least one required). Recipe ID is
the database primary key, not a separately editable field. Copy and
versioning/approval history remain out of scope.

## Master-data UI reference

The source describes four master-data modules:
- Customer
- Site
- Vehicle
- Driver

Customer:
- customer details
- projects
- production history

Site:
- project locations
- customer relationship
- site production

Vehicle:
- registration
- capacity
- driver assignment
- status
- history

Driver:
- driver details
- licence information
- assignments
- performance

The actual form fields must be taken from the supplied reference materials rather than invented.

**Update (2026-08-23), all at the user's explicit request:**
- "Customer" renamed to "Client" throughout (module, table, API, UI) - a new optional Address field was added at the same time, since it has no mockup backing.
- Every list screen (Client/Site/Vehicle/Driver/Header) now has a search box filtering the loaded rows client-side.
- Client/Site/Vehicle/Driver are grouped in the desktop nav under a "Resources" section heading; Dashboard, Header and Settings stay as flat top-level entries.

## UI implementation rule

Create reusable JavaFX components for the repeated master-data pattern:
- page title/header
- toolbar
- table
- search/filter area where required
- add action
- edit action
- delete action
- form/dialog
- validation message
- loading state
- empty state
- error state
- success notification

Keep the visual styling centralized in JavaFX CSS/theme resources.

## Header module

The supplied documents do not define Header - no schema or UI was created from assumptions. It stayed a clearly identified pending requirement until the user supplied the definition directly (2026-08-17); see docs/01_REQUIREMENTS.md. The list/form UI follows the same layout pattern as Customer/Site/Vehicle/Driver: a table (Company Name, Plant/Branch Name, Phone, Status) plus an Add/Edit dialog for all fields.
