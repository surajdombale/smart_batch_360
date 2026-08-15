# SmartBatch360 — Architecture Specification

## Source-defined architecture

The supplied documents explicitly describe:

JavaFX Desktop
→ API Client / REST
→ Spring Boot
→ Spring Data JPA
→ MySQL

The source also describes the future production flow:

PLC / Plant Equipment
→ Production/Backend layer
→ MySQL
→ REST API
→ JavaFX

Therefore, do NOT replace the documented REST architecture with an embedded Spring Boot desktop architecture unless the user explicitly changes this requirement.

## Technology stack

The supplied documents specify:
- Java 21
- JavaFX 21
- Spring Boot 3.x
- Spring Data JPA
- MySQL
- Flyway
- Jackson
- Maven
- PDFBox / iText
- Java HttpClient

For Phase 1 CRUD work, include only dependencies actually needed by the implemented functionality. Do not add reporting/PDF dependencies just because they appear in the long-term technology list.

## Recommended project separation

### Desktop client

Responsibilities:
- JavaFX screens
- navigation
- UI state
- form validation feedback
- API client
- DTO mapping
- user-friendly error handling

### REST backend

Responsibilities:
- REST endpoints
- validation
- service/business logic
- repository access
- MySQL persistence
- Flyway migrations

### Database

MySQL.

## Phase 1 flow

Customer example:

JavaFX Customer Screen
→ HTTP API client
→ Customer REST Controller
→ Customer Service
→ Customer Repository
→ MySQL

The same pattern should be used for:
- Site
- Vehicle
- Driver
- Header only after its requirements are defined

## Package recommendation

### Desktop

com.smartbatch360.desktop
- config
- api
- common
- navigation
- dashboard
- customer
- site
- vehicle
- driver
- header

### Backend

com.smartbatch360.api
- config
- common
- customer
- site
- vehicle
- driver
- header

Within each backend feature:
- controller
- dto
- entity
- repository
- service
- mapper where useful

## Important architecture rules

- Do not put database access in JavaFX controllers.
- Do not duplicate business validation only in the UI.
- API calls must not block the JavaFX UI thread.
- Keep REST DTOs separate from persistence entities.
- Keep API base URL configurable.
- Keep database credentials on the backend only.
- Do not expose MySQL directly to the desktop client.
- Do not hard-code secrets.
- Use Flyway for schema changes.

## Current development status from source

The documents state that:
- foundation/architecture is established
- backend API is established
- Batch Data/Reports backend is established
- dashboard API connection is working
- dashboard JSON mapping is fixed
- dashboard refresh works every 5 seconds
- Production UI is built
- Production backend connection is the next major task
- PLC integration is intentionally postponed

Claude must inspect the actual GitHub repository before assuming any of these components exist in the repository being built.
