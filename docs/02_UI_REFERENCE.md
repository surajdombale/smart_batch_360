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

Do not implement in the current phase.

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

The supplied documents do not define Header.

Do not create a Header schema or UI based on assumptions. Keep it as a clearly identified pending requirement.
