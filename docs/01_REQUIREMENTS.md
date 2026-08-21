# SmartBatch360 — Requirements Specification

## Source

Derived from the supplied SmartBatch360 project/review documents.

SmartBatch360 is an Industrial Batching Plant Management System intended for concrete batching plant production monitoring, batch history, reporting, recipe management, material consumption, analytics, and future PLC integration.

## Phase 1 implementation restriction

For the current development phase, implement only the first five requested data-management tables/modules plus the application shell/dashboard:

1. Customer
2. Site
3. Vehicle
4. Driver
5. Header — not defined in either supplied document; **clarified directly by the user on 2026-08-17** (see below)

Important: the supplied documents explicitly define Customer, Site, Vehicle and Driver as master-data modules, but do not define a "Header" master table. This was correctly flagged as a requirement clarification before any schema was created for it, and Header was NOT implemented until the user supplied its definition below.

## Header (clarified 2026-08-17, not from the source documents)

The user defined Header as: the company/plant letterhead block that will appear
at the top of printed Batch Logs and Order/Recipe reports, once those
(currently out-of-scope) modules are built. It has no dependency on
Batch/Recipe data - it is a standalone branding record.

- Multiple named headers are supported (e.g. one per plant/branch) - full CRUD, same pattern as the other four modules.
- Fields: Company Name (required), Plant/Branch Name (required), Address, Phone, Email, GSTIN/Tax ID (all optional), Status (Active/Inactive).
- No logo image upload in Phase 1 (text fields only, by user's choice).
- No foreign-key relationship to Customer/Site/Vehicle/Driver.

The current phase is CRUD-focused. Do not implement the broader production roadmap.

## Customer

The source documents describe Customer Management as:
- customer information
- related production/order history

For the current CRUD phase, use only fields explicitly available in the actual project requirements/UI reference. Do not invent a detailed customer schema from generic assumptions.

## Site

The source documents describe Site Management as:
- construction site information
- production details
- relationship to customers

Use only explicitly supplied fields for the CRUD schema.

## Vehicle

The source documents describe:
- vehicle details
- registration
- capacity
- driver assignment
- status
- history

Use only explicitly supplied fields for the current schema.

## Driver

The source documents describe:
- driver details
- licence information
- vehicle assignments
- production/trip history

Use only explicitly supplied fields for the current schema.

## Dashboard

The supplied documents describe a dashboard containing:
- Total Batches
- Today's Batches / today's production
- Today's Volume
- Total Recipes
- Total Vehicles
- Total Customers
- Revenue
- Plant Efficiency
- Production trend
- Recent activity / recent batches
- Backend status
- Database status
- API status
- PLC status
- Active Recipe
- Last Batch
- Average Batch Time
- Automatic refresh every 5 seconds

However, the current implementation phase must remain restricted to the five requested CRUD tables. Dashboard elements that require unimplemented production/recipe data should be represented only if their data source already exists; otherwise do not invent backend functionality.

## Broader modules documented by the source

The documents describe these future/application areas:
- Login
- Dashboard
- Production
- Batch Reports
- Material Consumption
- Recipe Management
- Customer Management
- Site Management
- Vehicle Management
- Driver Management
- Analytics
- Plant / PLC Monitoring
- Settings
- Alarm / Event History

Do not implement these future modules now except the allowed Dashboard and four defined master-data modules.

## Out of scope for this phase

Do not implement:
- Production controls
- PLC integration
- Material consumption
- Analytics
- Batch reports
- Recipe management
- Alarm/event history
- Settings
- User/role management
- Backup/restore workflows
- Advanced reporting
- Cloud synchronization
- Mobile application

The source explicitly states PLC integration is postponed until the software modules are stable.

## CRUD expectations

For each approved table:
- list records
- add record
- edit record
- delete record
- validate input
- show success/error feedback
- handle empty state
- handle loading state
- confirm destructive deletion

Search, sorting and pagination should be implemented only where supported by the UI/reference requirements for that screen.

## Source-of-truth rule

Functional requirements come from the requirements document.

Visual structure and appearance come from the UI reference document.

Do not silently invent missing requirements.
