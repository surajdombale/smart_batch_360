# Setting up SmartBatch360 on a new PC

You need one thing on the new PC besides MySQL: `SmartBatch360.exe` (bundles
its own Java runtime - Java doesn't need to be installed separately). MySQL
must already be installed and running - that's the only other prerequisite.
No manual SQL, no separate server process.

## 1. Install and launch

Unzip `SmartBatch360.zip` anywhere, then run `SmartBatch360.exe`. The app
opens straight away - the UI never waits on the database to start.

## 2. Connect the database (one-time)

Go to **Settings → Database Connection** and enter:
1. **Database URL** (host:port - e.g. `localhost:3306`)
2. **Username**
3. **Password**

Use a MySQL account with permission to create databases - your root/admin
login is the simplest choice. Click **Connect & Save**. SmartBatch360
creates its own `smartbatch360` database and tables automatically - nothing
to run by hand, and it never touches any other database on the server.

The connection is saved to your Windows profile
(`%APPDATA%\SmartBatch360\smartbatch360.properties`), not the install
folder, so it survives reinstalling or updating the app. Every launch after
this one connects automatically with no prompt. If you ever need to change
it (new password, different MySQL server), just go back to Settings →
Database Connection, update the fields, and save - the app tells you to
restart so the new connection takes effect.

Until a connection is saved, screens show their normal "can't reach the
server" error state with a Retry button - that's expected, not a bug; head
to Settings to fix it.

## Advanced / optional

- Default port for the embedded backend: **8081** (used internally between
  the UI and the embedded server, and by anything else that wants to call
  the REST API directly, e.g. `http://localhost:8081/api/v1/customers`). If
  it's taken on this PC, set `SERVER_PORT` before launching.
- Scripted/unattended deployments can skip the Settings screen entirely by
  setting `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
  environment variables before first launch, or by hand-editing
  `%APPDATA%\SmartBatch360\smartbatch360.properties` directly.
- `db/dev-setup.sql` is only useful if you'd rather pre-create a
  low-privilege scoped `smartbatch360` app user yourself and enter *that*
  into Settings instead of an admin account.
- Running the backend as its own standalone process (e.g. one central
  server that multiple desktop installs on other machines connect to over
  the network) is still possible via `scripts/package-backend.ps1` - point
  each desktop install at it with the `API_BASE_URL` environment variable
  instead of using its own embedded server.
