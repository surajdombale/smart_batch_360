# SmartBatch360 — CRUD Specification

## Scope

CRUD only for:
- Customer
- Site
- Vehicle
- Driver
- Header after requirements are clarified

## List

Each screen should:
- load records through the REST API
- show a clear table
- show loading state
- show empty state
- show API/database error state
- provide the actions supported by the reference

## Create

Flow:

Add
→ form
→ client-side validation
→ POST API
→ backend validation
→ database save
→ success notification
→ refresh list

## Edit

Flow:

Edit
→ load existing values
→ form
→ validation
→ PUT/PATCH API according to API conventions
→ save
→ success notification
→ refresh list

## Delete

Flow:

Delete
→ confirmation
→ DELETE API
→ backend validation/business rules
→ success/error notification
→ refresh list

Do not silently delete records.

## Validation

Validation should exist at:
- UI level for immediate feedback
- API/backend level for authoritative validation
- database level for constraints

## API errors

Map common HTTP errors to user-friendly messages.

Do not show raw stack traces or raw server exception responses.

## Responsiveness

Never perform HTTP calls synchronously on the JavaFX application thread.

Show a loading indicator while requests are running.

## Search/sorting/pagination

Implement only according to the actual requirements/UI reference for each table.

If the API does not currently support a required operation, do not fake it in the UI. Add the backend endpoint as part of the feature.
