# Operations Runbook

This runbook covers the hardened operational path for the Docker-based stack on Windows Command Prompt.

## 1. Bootstrap Secrets

Before the first `docker compose up`, create a local env file:

```cmd
copy compose.env.example .env
notepad .env
```

Change at least:

- `POSTGRES_PASSWORD`
- `DB_PASSWORD`
- `KEYCLOAK_BOOTSTRAP_ADMIN_PASSWORD`
- `KEYCLOAK_ADMIN_PASSWORD`
- `GF_SECURITY_ADMIN_PASSWORD`

Rules:

- keep `.env` local only; it is ignored by git
- do not commit database dumps or ad hoc env files
- use different values outside local/demo environments
- if you rotate database or Keycloak admin secrets, recreate the affected containers after updating `.env`

## 2. Start The Stack

```cmd
docker compose up -d --build
```

Verify the runtime:

```cmd
scripts\verify-runtime.cmd
```

## 3. Verify Flyway Migration State

Use this before upgrades, after restores, and after pulling new migrations:

```cmd
scripts\verify-migrations.cmd
```

What it checks:

- the compose PostgreSQL container is reachable
- `flyway_schema_history` exists
- there are no failed Flyway rows
- the database schema version matches the latest `V*__*.sql` file in the repo

If it fails because `flyway_schema_history` is missing, start the backend once so Flyway can apply the schema:

```cmd
docker compose up -d backend
```

## 4. Create A Backup

Create a timestamped SQL dump in the ignored `backups\` folder:

```cmd
scripts\backup-db.cmd
```

Or choose the output path explicitly:

```cmd
scripts\backup-db.cmd backups\library-before-upgrade.sql
```

Notes:

- the dump is plain SQL and includes `DROP` statements for clean restore
- the dump contains live data and should be treated as sensitive
- the script reads connection settings from `.env`, or falls back to `compose.env.example`

## 5. Restore A Backup

Recommended sequence:

1. Stop the backend so it is not serving traffic during restore.
2. Restore the dump.
3. Verify migrations.
4. Verify runtime.

Commands:

```cmd
docker compose stop backend
scripts\restore-db.cmd backups\library-before-upgrade.sql
scripts\verify-migrations.cmd
docker compose up -d backend frontend
scripts\verify-runtime.cmd
```

To skip the confirmation prompt:

```cmd
scripts\restore-db.cmd backups\library-before-upgrade.sql -Force
```

## 6. Recommended Pre-Upgrade Flow

Before applying a branch or release with new migrations:

1. `scripts\backup-db.cmd`
2. pull the code change
3. update `.env` if new operational settings were introduced
4. `docker compose up -d --build`
5. `scripts\verify-migrations.cmd`
6. `scripts\verify-runtime.cmd`

## 7. Secret Hygiene

Minimum local practice:

- keep `compose.env.example` unchanged as a sample only
- keep real values in `.env`
- do not reuse the shipped sample passwords beyond local/demo use
- rotate secrets before sharing a stack outside your machine
- rotate again after public test sessions if admin endpoints or dumps were exposed

Related docs:

- [README.md](../README.md)
- [SETUP_GUIDE.md](../SETUP_GUIDE.md)
- [scripts/README.md](../scripts/README.md)
