# Setting up SmartBatch360 on a new PC

You need two things on the new PC, in this order. Nothing besides MySQL is
required - both packages bundle their own Java runtime.

## 1. Database (one-time)

MySQL must already be installed and running on this PC. Run the setup script
once, as a user with admin rights (e.g. via MySQL Workbench, or):

```bash
mysql -u root -p < db/dev-setup.sql
```

`db/dev-setup.sql` is included alongside `SmartBatch360-Server.exe` in the
server package. It only creates a `smartbatch360` database and a scoped
`smartbatch360` app user - it does not touch anything else.

## 2. Start the backend server

Unzip `SmartBatch360-Server.zip` anywhere, then run `SmartBatch360-Server.exe`.
A console window opens and stays open while the server runs - closing it stops
the server. On first run it automatically creates the database tables.

- Default port: **8081**. If that's also taken on this PC, set an environment
  variable before launching: `set SERVER_PORT=8082`
- If MySQL needs different credentials than the defaults (`smartbatch360` /
  `smartbatch360`), set `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`,
  `DB_PASSWORD` environment variables before launching.

## 3. Start the desktop client

Unzip `SmartBatch360-Desktop.zip` anywhere, then run `SmartBatch360.exe`.

- It talks to `http://localhost:8081` by default (matches step 2's default
  port). If you changed `SERVER_PORT`, set an environment variable before
  launching the desktop app too: `set API_BASE_URL=http://localhost:<port>`

That's it - both windows can be closed and reopened independently; data
persists in MySQL between runs.
