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
- Batch Reports — built 2026-08-24: search/filter (batch number range, date range, Client/Site/Vehicle/Driver/Recipe)/pagination/sorting/detail viewing. PDF/Excel/print export explicitly excluded from this pass by the user's own scoping decision - see "Do not build now" below
- Material Consumption — built 2026-08-26: target vs achieved vs variance/wastage, aggregated by day/week/month, built from existing Batch/BatchMaterial data (no new entity). First-pass scope chosen by the user: the aggregated table only - charts are a later pass, see "Do not build now" below

- Material Management + Order flow — built 2026-08-27 at the user's request. Materials became first-class records (name + unit KG/LITRE + density) instead of free text repeated on every recipe row; Recipe now references them and DERIVES its total batch quantity in m3 from them rather than accepting a typed-in figure; Order (sales order: Customer/Site/Recipe/quantity in m3) was added, created UNFULFILLED. Material Consumption gained an order-based projection (Order -> Recipe -> Recipe Materials -> Material) alongside the existing batch-history aggregation, which was left untouched.
  - Density is user-entered per material because KG cannot be converted to m3 without it and nothing in the schema carried one; hardcoding assumed densities was explicitly ruled out by the user.
- Stabilisation pass — 2026-08-27: full test sweep of every tab and dialog. Fixed malformed requests returning 500 instead of 4xx, a connection error shown on every launch (backend boots slower than the UI), and truncated dashboard KPI labels. Packaged as V3 (3.0.0).

- Order lifecycle — built 2026-09-05, completing the module deferred on 2026-08-27. Statuses UNFULFILLED -> IN_PROGRESS -> FULFILLED, with CANCELLED reachable from either non-terminal state; terminal states cannot be left. Unlike Batch's deliberately permissive controls (which stand in for hardware that isn't wired up), these transitions are ENFORCED - an order's status is a business record. An in-progress order cannot be deleted, only cancelled.

- Batch -> Order linking — built 2026-09-06, the pass held back on 2026-09-05 as the natural follow-on to the lifecycle. A batch may now name the order it was produced against (optional: ad-hoc batches are still normal), and an order reports produced vs remaining m3 summed from those batches, so fulfilment is MEASURED rather than asserted. The link is guarded: the batch's recipe, customer and site must match the order's, because a mismatch would corrupt both the fulfilment figures and the order's projected material consumption. An order with batches recorded against it can no longer be deleted.
  - Fixed while verifying: the dashboard's startup retry gave up after 30s against a cold boot measured at 32.4s, so a backend that was seconds from answering was reported as unreachable. The retry now runs until startup actually settles.
  - Also found by looking at the running screens rather than the API: separate Produced/Remaining columns took Orders to nine and ellipsised every one of them, so the two figures now share one "0 of 10 m3" column; editing a batch whose order had since been fulfilled silently unlinked it on save; and SiteDto/VehicleDto had no toString, which both dumped raw records into the batch form's ComboBoxes and squeezed all twelve of its field labels down to "...".


- API error messages were never reaching the user — 2026-09-07. ApiErrorDto did not declare the timestamp ApiError sends and did not ignore unknown properties, so Jackson threw on EVERY error body and ApiClient substituted generic per-status text ("The submitted data is invalid."). Every explanatory message the backend produces - order lifecycle rules, batch/order mismatch guards, delete conflicts - was being discarded app-wide. Found because a recipe save reported nothing useful about a material with no density.

- Recipe materials as line items — 2026-09-07, reported as a bug: adding ingredients to a recipe and entering their values did not work. The materials were an editable TableView, whose cells only write back to the model on Enter - pressing Save cancelled the edit and threw the typed quantity away, so the recipe was rejected for missing a value that was on screen. Rebuilt as line items (header row, one row per material, "+ Add material", running total) after the Stripe invoice editor the user supplied as reference. Every control is live and bound to its row, so there is no edit mode to commit or lose, and each row shows its own m3 contribution.

- Everything measured in kilograms — 2026-09-07 at the user's request, replacing the unit + density model from 2026-08-27. Materials carried their own unit (KG/LITRE) and a density, and a recipe derived its batch size by converting each line to m3; orders and batches were then sized in m3 against recipes whose ingredients are weighed in kg on the plant floor, which is where processing an order into batches came apart. Material.unit and Material.density_kg_per_m3 are dropped, a recipe's batch size is the plain sum of its lines, and orders/batches are kg. The order-consumption projection is unchanged - it was always the ratio of order quantity to batch quantity, which is unit-agnostic.
  - This also removed a failure mode rather than just a unit: a KG material was unusable in any recipe until someone supplied a density, so 'OPC S3 Cement' and 'Plasticizer' could not be used at all.
  - Recipe totals were RECOMPUTED from their lines, not converted - the old m3 figures cannot be converted back once the densities are gone. Order quantities kept their numbers and read as kg, so pre-existing orders need reviewing by hand.
  - Vehicle capacity stays in m3: a mixer drum is a volume.

- Material Consumption chart — 2026-09-11, the pass deferred on 2026-08-26. One chart above the existing table: target vs achieved per material, summed across whatever the page's filters select. It is an emphasis chart, not a colourful one - Achieved is the series that matters (accent blue), Target is context (gray) - because the question it answers is "which materials came in over or under target", and the table beneath stays as its per-period breakdown and accessible twin. Colours were checked with a colour-blindness validator rather than by eye; bars are capped at 24px so a short material list doesn't balloon into slabs; each bar has a hover tooltip with achieved / target / variance.

- Regression pass — 2026-09-12, over everything that changed since V3 (kg switch, recipe line items, order lifecycle and fulfilment, batch-to-order link, the consumption chart). 114 backend tests plus 19 live checks against the real database all passed, so the only fix needed was on Production: its table clipped Status to "STOPP..." while the Controls column held a wide empty stripe - the same CONSTRAINED-policy problem the Orders table had, fixed the same way.

- Batch Reports PDF export — 2026-09-12, after the user lifted the deferral (and with it the architecture doc's rule against reporting dependencies, for reporting only). An Export PDF button beside Search/Reset writes the whole filtered result to a landscape A4 table - re-fetched rather than taken from the table, since the table only holds the page being viewed. The filters in force are printed under the title so a saved report states what it covers, and a row cap of 2000 is admitted in the document rather than trailing off silently.
  - PDFBox (Apache-2.0), not OpenPDF (LGPL): the app is delivered to a customer, so the licence matters more than OpenPDF's easier table API. The table is drawn by hand as a result, which is why it has tests - pagination, over-long values, and characters the standard-14 fonts cannot encode (showText throws on those, which would have failed a whole export over one customer name).
  - Excel export is the next chunk; print follows from the PDF.

- Batch Reports Excel export — 2026-09-13, the second half of the export pass. An Export Excel button beside Export PDF writes the same filtered result, through the same fetch-then-choose-a-file flow (now parameterised by format rather than duplicated). The reason for a spreadsheet beside the PDF is that figures stay figures, so quantities are real numeric cells and cycle times real dates - never the formatted strings the PDF prints - which is what lets someone sort, filter and total them. Three sheets: Batches (header frozen, autofilter on), Materials (target / setpoint / achieved / variance per line) and About (generated time, filters, any row-cap note), kept separate so both tables start at row 1.
  - fastexcel (Apache-2.0) rather than Apache POI: the app only writes spreadsheets, and POI's .xlsx support would ship several MB of reading and formula code in every install.
  - Control characters are stripped from text cells: an .xlsx is XML inside, XML 1.0 cannot hold them, and one in a customer or site name would make Excel reject the whole file as corrupt.
  - Checked against the real API data, not just test rows - including the historical batch whose water was recorded in litres, which exports with its unit intact.

- Batch Reports print — 2026-09-14, completing the export line (PDF, Excel, print). A Print button beside the two export buttons sends the report to the standard system print dialog. It prints the very document the PDF export writes, through PDFBox's own printing support, so the paper and the PDF cannot disagree and there is no second layout to keep in step - and no new dependency, since PDFBox was already here and java.awt.print ships with the JDK.
  - The print dialog is AWT and modal, so it runs on a worker thread; holding the JavaFX thread on it would freeze the window behind it.
  - Fixed in passing: the export worker read the filter controls off the JavaFX thread to describe the filters. It happened to work, but it was not safe; both export and print now read them on the FX thread before handing off.

- Packaged as V5 (5.0.0) — 2026-09-18, for client review: everything since V4 (consumption chart, Production column fix, Batch Reports PDF/Excel export and printing). Before packaging, all three report buttons were clicked through in the running app for the first time, which caught a bug no test could: Spring Boot had switched the whole JVM to headless AWT, so Print threw HeadlessException. Fixed by starting the embedded backend non-headless.

- Hardening: values that do not fit the database — 2026-09-20. Probing the API with well-formed requests carrying out-of-range numbers found them coming back as 409 "This record conflicts with an existing one or is referenced elsewhere", which is simply untrue: nothing conflicted, the number had too many digits for its column. Over-long names were already correct (400), because they carry @Size; the numeric fields had minimums but no maximums.
  - Digit limits now match the columns on all seven numeric request fields (recipe line, batch target/produced, batch material target/setpoint/achieved, order quantity), so these are rejected as a field-level 400 naming the limit before reaching the database.
  - As a safety net for anything the annotations cannot cover (a derived total, say), the data-integrity handler now separates SQLState class 22 - a data exception, i.e. the caller's value - from a genuine constraint clash, returning 400 rather than 409. Duplicates are still 409 with their own message.

- Hardening: form errors for list lines — 2026-09-21, the desktop half of the previous day's fix. The backend reports a bad recipe or batch line as "materials[0].quantity", but forms register the list, not each line, so these fell through to the form-level message - where each one replaced the last. Two bad lines meant only the second was ever shown, and none said which line was meant. They are now collected and shown together, labelled "Line 1:", "Line 2:" and so on.

- Hardening: behaviour at real data volume — 2026-09-24. Everything until now had been checked against a single batch. Seeded 4800 throwaway batches (since removed) and measured: Batch Reports 28ms, Material Consumption 50ms and the Dashboard 26ms all held up, because they are paginated or aggregated. Production did not: it loaded every batch ever made, with all its materials, on every open - 3.3 MB and 1.7-2.8 seconds at 4800, growing without limit. A plant making 50-200 batches a day reaches that within a year.
  - Production now loads the 200 most recent batches (newest first, the search endpoint's own default order) and says so, pointing at Batch Reports for the rest: 0.054s and 150 KB, and flat as history grows. Its search box now filters those 200 rather than everything, which is what the note explains.
  - The 2000-row export cap also behaved correctly at volume, reporting "the first 2000 of 4801" in the exported file.

- Hardening: the order list's query count — 2026-09-25, following the same question as yesterday to the other screen that grows per delivery. Of the eight screens that load their whole list, seven hold reference data that stays small; Orders does not. It was worse than Production's payload problem: the list shows how much has been produced against each order and asked for that number one order at a time, so the query count tracked the order count exactly - 2,002 orders, 2,002 queries. The customer, site and recipe names on each row are lazy associations, adding one query per distinct row on top.
  - Measured against the real database with 2,002 seeded orders spread over 100 customers (all since removed): 2,213 queries and 2.0s before, 8 queries and 0.08s after. Fulfilment is now one grouped query and the list fetches the three names with it; the single-order endpoint still sums one order on its own, which is correct there.
  - Both new tests were checked against the old code before being kept: the query-count test counted 65 instead of 11, and the fetch test found the associations uninitialised. A test that passes either way is worth less than no test.
  - Orders still loads every row, unlike Production. At 2,002 orders that is now 0.08s, so the query count was the defect worth fixing; a row cap can wait until the payload itself is the problem.

- Hardening: the batch read path — 2026-09-26, the same question one level down. A batch row shows six names (recipe, order, customer, site, vehicle, driver) and carries its list of materials, all lazy, so reading a list of batches cost a query per row. Measured at 3,000 batches over 100 customers: opening a page of Batch Reports took 109 queries and 0.44s, and the 2,000 rows an export re-fetches took 2,413 queries and 2.7-4.8s. With @BatchSize on the mappings: 13 queries and 0.035s, 36 and 0.19s. Production's 200-row load benefits too, at 14 queries.
  - An entity graph on the search query was the obvious fix and was rejected on measurement: it cut the page to 9 queries but made it three times slower (0.03s to 0.8s), because joining six tables before the sort and limit costs more than the round trips it saves. Worth remembering the next time a query count looks like the thing to optimise.
  - The setting lives on the mappings, not in application.yml as hibernate.default_batch_fetch_size. The test profile replaces that file wholesale, so a value there cannot be tested, and the test would have been guarding the test config rather than the app. Annotations travel with the entities and BatchSearchQueryCountTest fails if they are removed.
  - Separately, cycle_date_time had no index, though both batch screens sort by it - a full scan plus filesort on every open. V9 adds one. At 3,000 rows it changed nothing measurable; at 30,000 (about six months at 200 batches a day) Production went from 0.15-0.78s to 0.03-0.05s. Deep pages still do not benefit, since a large OFFSET walks the index anyway.
  - Seeded 30,000 batches with 180,000 material rows for this, all since removed. One seeding mistake is worth noting: the equipment-status columns are plain VARCHAR, so rows with a value the enum does not have ('OFF') loaded fine and then failed every read with a 500. The API is the only writer, so this is not a live defect, but the schema does not stop it.

- Hardening: the aggregate screens — 2026-09-27, the last read paths not yet measured at volume. Their cost grows with total history rather than page size, so they were the remaining unknown. The Dashboard is fine at 30,000 batches (13 queries, 0.03s) because it aggregates in SQL. Material Consumption was not: with no date filter it covers every batch ever made, and it loaded whole BatchMaterial and Batch entities to read five fields from each - 180,002 entities, 2.8-3.8s. It now aggregates from projections: 0.78-1.8s at the same 6 queries, 0.25s for a one-month filter.
  - The aggregation stays in Java. Pushing the day/week/month bucketing into SQL would tie the buckets to the database session's time zone, which is SYSTEM (IST) here while the bucketing is UTC - so the same rows would land in different days. Left alone deliberately; see the note below.
  - Still grows with history. At two years of 200 batches a day (about 876,000 material rows) an unfiltered open would be several seconds again. The remaining options are a default date range on the screen or SQL-side grouping, and the first is a decision about what operators should see, so it is the user's call rather than a silent change.
  - Two defects found on the way, both pre-existing and neither caused by the performance work:
    - The grouping key joined material name and period with a literal NUL character. It worked, but a NUL makes the whole file binary to git and grep: every change to MaterialConsumptionService showed as "Binary files differ" with no reviewable diff. Now a record key.
    - A group's unit came from whichever row created it, so a period whose rows disagree reported a different unit depending on database row order. The real database still has one water line carrying "L" from before the kilograms switch, which is what exposed it - the projection change altered the row order and flipped that group from "L" to "kg". Now the unit most rows agree on. This was only visible because the change was verified by comparing full API responses before and after, not just by running the tests.
  - The "L" row itself is left alone: relabelling it as kilograms would need a density, and this project does not assume one. Flagged for the user to decide, alongside the legacy pre-kg order quantities on orders #1 and #9.

- Regression pass over the week's query changes, and a timestamp bug it found — 2026-09-28. Everything changed this week was query-level (@BatchSize on seven entities, the V9 index, the grouped fulfilment sum, the consumption projection), and the V9 index in particular changed query plans, so sorting and filtering needed re-checking against data with actual variety. Seeded 500 batches over three customers and 100 days, since removed, and checked 14 things: default and explicit sort order, sort by batch number and produced quantity, date/number/client filters against SQL counts, page disjointness, and that every row still carries its six names and its materials. Exports were measured at the 2,000-row cap too: PDF 0.74s for 80 pages, Excel 0.44s, 51 MB of heap, and the files contain all 2,000 rows, 12,000 material rows, the filter line and the cap note.
  - One check failed: the date filter returned 149 rows where SQL counted 148. Chasing that found a real bug, present since the first migration. Cycle times were stored 5:30 out. Hibernate hands the driver a timestamp with no zone and the driver read it as the MySQL session's zone, which was SYSTEM - serverTimezone=UTC in the JDBC URL only tells the driver what to assume and never sets the session. So an Instant of 10:00Z was written as 10:00 local and stored as 04:30Z.
  - Reads applied the same shift in reverse, so the app was self-consistent and the desktop showed the right local time. That is exactly why no test and no screen caught it: the only way to see it was to compare what the app said against what the table held. The cost was real anyway - the API labelled local wall-clock times as "Z", and the customer's own SQL, an export, or any future integration would read times that never happened.
  - Fixed with connectionTimeZone=UTC and forceConnectionTimeZoneToSession, so the session is UTC and what Hibernate sends means what it says. Verified end to end: a batch posted as 10:00Z now stores 10:00 rather than 04:30, a server-stamped one stores the true clock time, and the date filter agrees with SQL exactly. hibernate.jdbc.time_zone=UTC was tried first and measured to have no effect here.
  - Rows written before the fix are now read as true UTC, so their displayed time moves 5:30 earlier. That affects the one real batch and its two material lines in the user's database. Correcting them means assuming which zone they were written in, which is the user's call - the same bucket as the legacy "L" unit and the pre-kg quantities on orders #1 and #9.
  - Nothing else regressed: 14/14 checks passed after the fix, the write paths (create, update, start/pause/resume, delete) all work, both fulfilment paths agree with SQL, and the user's own row came back byte-identical to the pre-pass backup.

- Report dates mean days at the plant — 2026-09-30, following through on the timestamp fix. Making the stored instants true UTC was right, but it changed what the four date conversions meant. They were written with UTC, and that had been accidentally correct: the app's instants were local wall-clock labelled Z, so reading a day in UTC gave the local day. Once storage was fixed, UTC conversion started slicing on UTC boundaries.
  - For a plant five and a half hours ahead of UTC that broke the night shift. A batch made at 01:30 was filed under the previous day, and filtering Batch Reports to the day it was actually made returned nothing. Checked against the running app: two batches that both landed in the 25th now land in the 25th and the 26th, and the filter that returned 0 rows returns the batch.
  - All four conversions now live in ReportingZone - the only place a date becomes an instant or an instant becomes a day. Configurable with smartbatch360.reporting.zone, defaulting to the zone the server runs in, which for a single-plant install is the plant's own.
  - The zone is injected rather than read from config inside the conversion, which is what makes it testable: the tests pin behaviour for a real plant zone (Asia/Kolkata) instead of whatever the build machine is set to. The existing tests pass UTC explicitly and keep their meaning. All seven new tests fail against the UTC version - checked, not assumed.
  - Worth recording as a pattern: fixing the storage layer moved a bug rather than removing it, because two wrongs had been cancelling. The first fix was verified in isolation and looked complete; only asking what else depended on the old behaviour found this. 132 tests now.

### Do not build now
- Analytics
- PLC Monitoring
- Settings (beyond the Database Connection tab noted above - Plant info, PLC communication, backup/restore, user management, general preferences)
- Alarm/Event History
- advanced authentication/permissions
- backup/restore

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
