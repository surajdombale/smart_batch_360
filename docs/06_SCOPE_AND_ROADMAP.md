# SmartBatch360 — Scope Control and Roadmap

## Current phase

The current phase is intentionally small:

### Build now
- application shell
- dashboard foundation
- Client CRUD (renamed from "Customer" 2026-08-23 at the user's request, plus a new optional Address field - docs/01_REQUIREMENTS.md)
- Site CRUD
- Vehicle CRUD
- Driver CRUD
- Header CRUD — clarified by the user 2026-08-17 (docs/01_REQUIREMENTS.md), now built
- Settings → Database Connection only — explicitly requested by the user 2026-08-21 as part of merging the app into a single install (docs/03_ARCHITECTURE.md); the rest of Settings remains out of scope below
- Search on every list screen, and grouping Client/Site/Vehicle/Driver under a "Resources" nav section — both requested by the user 2026-08-23 (docs/02_UI_REFERENCE.md)
- Recipe Management CRUD — built 2026-08-23 as a prerequisite for Production, which the user chose as the next module to build (docs/02_UI_REFERENCE.md)
- Production — built 2026-08-23, the user's chosen next module. Real Batch CRUD (not just a UI mock) referencing Client/Site/Vehicle/Driver/Recipe, with manual/simulated equipment status and batch controls (no PLC integration - still intentionally postponed). Delete-conflict guards added to all five referenced modules

### Do not build now
- Batch Reports
- Material Consumption
- Analytics
- PLC Monitoring
- Settings (beyond the Database Connection tab noted above - Plant info, PLC communication, backup/restore, user management, general preferences)
- Alarm/Event History
- advanced authentication/permissions
- backup/restore
- reporting/export

## Future roadmap from supplied documents

The supplied documents identify:
1. Dashboard completion
2. Production integration
3. Material Consumption
4. Reports enhancement
5. Recipe Management integration
6. Customer/Site/Vehicle/Driver management
7. Analytics
8. Alarm/Event History
9. PLC integration
10. Testing/logging/backup/restore/security/permissions
11. Final installer/deployment

## PLC rule

PLC integration is intentionally postponed until software modules are stable.

Do not introduce PLC dependencies into Phase 1 CRUD modules.

## Future production data

The documents describe BATCH_DATA with:
- batch number
- batch quantity
- per-cycle quantity
- customer/site
- driver/vehicle
- cycle date/time
- cycle number
- shift
- recipe/order
- target/setpoint/achieved totals
- Material 1 through Material 20 with name/target/setpoint/achieved

Keep Phase 1 architecture clean enough to add this later.
