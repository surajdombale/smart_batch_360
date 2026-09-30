# SmartBatch360 — Claude Instructions

Read these files before making any implementation changes:

- docs/01_REQUIREMENTS.md
- docs/02_UI_REFERENCE.md
- docs/03_ARCHITECTURE.md
- docs/04_DATABASE.md
- docs/05_CRUD_SPECIFICATION.md
- docs/06_SCOPE_AND_ROADMAP.md
- docs/07_GIT_AND_DEVELOPMENT.md
- prompts/MASTER_PROMPT.md
- source/UI_REFERENCE_ORIGINAL.docx
- source/REQUIREMENTS_ORIGINAL.docx

## Current scope

docs/06_SCOPE_AND_ROADMAP.md is the live record of what is built, what is
deliberately deferred, and why. Read it rather than assuming a phase: the
first-phase list (Dashboard plus Client/Site/Vehicle/Driver CRUD) was completed
long ago, and Recipe Management, Production, Batch Reports with PDF/Excel
export and printing, Orders with fulfilment, and Material Consumption were all
built afterwards at the user's request.

Its "Do not build now" section is the current answer on Analytics, PLC
monitoring, Alarm/Event History, Settings beyond the Database tab,
authentication and backup/restore. Do not start any of those without asking.

## Standing constraints

These came from the user directly and still hold:

- **Kilograms only.** Materials and recipes are in kg (migration V8). Never
  introduce a density or a volume conversion, and never assume one to correct
  old data - ask.
- **Dates mean days at the plant.** Timestamps are stored as true UTC instants;
  every conversion between an instant and a calendar day goes through
  ReportingZone. See the rule in docs/03_ARCHITECTURE.md.
- **Do not change the Resources tables or their behaviour** unless asked.
- **Flyway owns the schema.** Migrations are additive; never edit an applied one.
- **Commit messages carry no `Co-Authored-By` trailer.**
- **Push each commit on its own** - commit, push, commit, push.

## Architecture

Follow the documented architecture:

JavaFX Desktop
→ REST API
→ Spring Boot
→ Spring Data JPA
→ MySQL

Do not invent a different architecture. docs/03_ARCHITECTURE.md carries the
rules that have been learned since, including that a list endpoint must not
cost a query per row.

## Important

The requirements document is the functional source of truth.

The UI reference document is the visual source of truth.

Do not invent missing business fields.

Verify changes against the running app and the real database, not only against
the test suite. Several bugs here were invisible to tests and only appeared
when the app's output was compared with what the database actually held.
