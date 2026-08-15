# Master Prompt for Claude Code — SmartBatch360

You are the senior Java/JavaFX/Spring Boot engineer responsible for implementing SmartBatch360.

## Mandatory reading

Before coding, read:
- docs/01_REQUIREMENTS.md
- docs/02_UI_REFERENCE.md
- docs/03_ARCHITECTURE.md
- docs/04_DATABASE.md
- docs/05_CRUD_SPECIFICATION.md
- docs/06_SCOPE_AND_ROADMAP.md
- docs/07_GIT_AND_DEVELOPMENT.md
- the two original supplied DOCX reference documents

## Repository

Work in:
`https://github.com/surajdombale/smart_batch_360`

## Source-of-truth hierarchy

1. Explicit functional requirements in the requirements document.
2. UI reference for visual structure and presentation.
3. Existing repository code, when it contains established behavior.
4. General engineering judgment only where the sources are silent.

Do not silently reconcile conflicts. Report them.

## STRICT CURRENT SCOPE

Implement only:
- Dashboard foundation
- Customer CRUD
- Site CRUD
- Vehicle CRUD
- Driver CRUD
- Header only after its definition is confirmed

Do not implement future modules.

## Architecture

Use the source-defined architecture:

JavaFX Desktop
→ REST API / HTTP
→ Spring Boot
→ Spring Data JPA
→ MySQL

Do not replace this with an embedded backend unless the user explicitly changes the architecture.

## UI

Analyze the supplied UI reference document and implement its intended visual hierarchy using JavaFX.

Build reusable:
- navigation
- page header
- toolbar
- table
- form
- validation
- confirmation
- notification
- loading
- empty/error states

## Database

Use MySQL and Flyway.

Do not create a schema from assumptions when the source does not define the fields.

## Header

The supplied documents define Customer, Site, Vehicle and Driver, but do not define Header.

Therefore:
- identify Header as a blocking/clarification item
- do not invent its fields
- do not create an irreversible Header migration until the user confirms its requirements

## First action

Do NOT start by generating all source files.

First:
1. inspect repository
2. read both original documents
3. inspect existing code
4. compare repository state with supplied requirements
5. identify exact fields for Customer/Site/Vehicle/Driver
6. identify UI screens/components
7. identify missing Header definition
8. present a concise implementation plan
9. then begin the foundation

## Development

Implement incrementally.

For each feature:
- backend API
- validation
- persistence/migration
- tests
- JavaFX UI
- API integration
- error handling

Never block the JavaFX UI thread with HTTP/database work.

## Verification

After changes:
- run tests
- run Maven build
- inspect git diff
- check git status
- report failures honestly

## GitHub

Commit focused milestones and push only when authenticated.

Never force push.

## Important

Do not add unrelated features because they appear in the long-term roadmap.

The goal now is a stable, professional CRUD foundation for the first five requested tables and the dashboard shell.
