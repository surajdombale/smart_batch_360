# Setting up SmartBatch360 on a new PC

You need two things on the new PC, in this order. Nothing besides MySQL is
required - both packages bundle their own Java runtime.

## 1. Database (one-time, no manual script needed)

MySQL must already be installed and running on this PC - that's it. You do
**not** need to create a database or run any SQL yourself; the server does
that for you the first time it starts (step 2).

## 2. Start the backend server

Unzip `SmartBatch360-Server.zip` anywhere, then run `SmartBatch360-Server.exe`.

**First run only:** a small window appears asking for:
1. **Database URL** (host:port - e.g. `localhost:3306`)
2. **Username**
3. **Password**

Use a MySQL account with permission to create databases - your root/admin
login is the simplest choice. SmartBatch360 creates its own `smartbatch360`
database automatically and never touches any other database on the server.
This is asked only once - the connection is saved to `config/` next to the
`.exe`, and every run after that starts straight up with no prompt. If the
saved connection ever stops working (password changed, MySQL moved), the
same prompt reappears automatically so you can re-enter it.

A console window opens and stays open while the server runs - closing it
stops the server.

- Default port: **8081**. If that's also taken on this PC, set an environment
  variable before launching: `set SERVER_PORT=8082`
- Advanced/scripted deployments can still skip the prompt entirely by setting
  `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` environment
  variables before first launch, or by hand-editing
  `config/smartbatch360.properties` (created after the first run).
- `db/dev-setup.sql` (also included) is now optional - only useful if you'd
  rather pre-create a low-privilege scoped `smartbatch360` app user yourself
  and enter *that* into the prompt instead of an admin account.

## 3. Start the desktop client

Unzip `SmartBatch360-Desktop.zip` anywhere, then run `SmartBatch360.exe`.

- It talks to `http://localhost:8081` by default (matches step 2's default
  port). If you changed `SERVER_PORT`, set an environment variable before
  launching the desktop app too: `set API_BASE_URL=http://localhost:<port>`

That's it - both windows can be closed and reopened independently; data
persists in MySQL between runs.
