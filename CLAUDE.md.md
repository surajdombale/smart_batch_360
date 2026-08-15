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

For the first development phase implement ONLY:

- Dashboard
- Customer CRUD
- Site CRUD
- Vehicle CRUD
- Driver CRUD

The Header module must NOT be implemented until its requirements are clarified.

Do NOT implement:

- Production
- PLC
- Recipe Management
- Material Consumption
- Reports
- Analytics
- Alarm/Event History
- Settings
- Other future modules

## Architecture

Follow the documented architecture:

JavaFX Desktop
→ REST API
→ Spring Boot
→ Spring Data JPA
→ MySQL

Do not invent a different architecture.

## Important

The requirements document is the functional source of truth.

The UI reference document is the visual source of truth.

Do not invent missing business fields.

Before implementation, inspect the existing repository and present an implementation assessment.

Do not modify code until the user approves the assessment.