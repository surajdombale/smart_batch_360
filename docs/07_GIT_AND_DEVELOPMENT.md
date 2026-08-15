# SmartBatch360 — Git and Development Workflow

## Repository

GitHub repository:
`https://github.com/surajdombale/smart_batch_360`

## Before implementation

1. Clone/open the repository.
2. Inspect current branch and files.
3. Do not overwrite existing code blindly.
4. Confirm whether the repository already contains the backend or desktop foundation.
5. Read all project docs.

## Development workflow

For each feature:
1. Define requirements.
2. Define API contract.
3. Define DB migration.
4. Implement backend.
5. Test backend.
6. Implement JavaFX client.
7. Test UI behavior.
8. Build the application.
9. Inspect git diff.
10. Commit.

## Commit style

Use focused messages, for example:
- `feat: add customer CRUD`
- `feat: add vehicle management`
- `fix: handle duplicate driver license`
- `test: add site service tests`

## Security

Never commit:
- MySQL passwords
- API keys
- tokens
- private certificates
- local secrets

Use environment/configuration files that are ignored by Git.

## Push

Only push after:
- build succeeds
- relevant tests pass
- git diff is reviewed
- secrets are absent

Never force-push.

Never claim GitHub push success unless the command actually succeeds.
