# Maintenance Scripts

## Reset Book View Counts

Run this from the project root:

```powershell
.\scripts\reset-book-viewcounts.ps1
```

Notes:

- The script checks for a running Docker Compose `postgres` service first and falls back to local `psql`.
- Force a specific mode with `-Mode docker` or `-Mode local`.
- Preview the action without changing data with `-WhatIf`.
- This resets counts by deleting `VIEWED` rows from `activity_log`, so those book-view entries also disappear from activity history.

## Public Test Setup

Use this from Windows Command Prompt with one tunnel only.

Start one HTTPS ngrok tunnel to local port `3000`:

```cmd
ngrok http http://localhost:3000
```

Then run:

```cmd
scripts\start-public-test.cmd -PublicUrl https://your-ngrok-url
```

Or use the env file:

```cmd
copy scripts\public-test.env.example .env.public-test
notepad .env.public-test
scripts\start-public-test.cmd
```

Tunnel only `3000`. Do not expose `8080` or `8081`. The script rewrites `.env.public-test`, recreates `keycloak`, `backend`, and `frontend`, routes API and Keycloak traffic through the frontend nginx, then updates the Keycloak `library-web` client redirect URIs and web origins for the public URL.

Preview the changes without touching containers:

```cmd
scripts\start-public-test.cmd -PublicUrl https://your-ngrok-url -WhatIf
```

Detailed how-to: `docs/NGROK_WINDOWS_SINGLE_PORT_SETUP.md`

## Runtime Verification

Verify the documented runtime URLs with one command:

```cmd
scripts\verify-runtime.cmd
```

If you are running the Vite frontend locally on `5173` instead of the Docker frontend on `3000`:

```cmd
scripts\verify-runtime.cmd vite
```
