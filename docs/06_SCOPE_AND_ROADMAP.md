# SmartBatch360 — Scope Control and Roadmap

## Current phase

The current phase is intentionally small:

### Build now
- application shell
- dashboard foundation
- Customer CRUD
- Site CRUD
- Vehicle CRUD
- Driver CRUD
- Header only after clarification

### Do not build now
- Production
- Batch Reports
- Material Consumption
- Recipe Management
- Analytics
- PLC Monitoring
- Settings
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
