# Windows ngrok Setup (One Port Only)

This guide is for Windows Command Prompt, including the IntelliJ terminal. It uses exactly one `ngrok http` tunnel.

Use this tunnel only:

- `http://localhost:3000`

Do not expose these directly:

- `http://localhost:8080`
- `http://localhost:8081`

Why this works:

- the frontend container listens on `3000`
- after public-test setup is applied, the frontend proxies `/api` to the backend
- the same frontend also proxies `/auth` to Keycloak
- testers only need one HTTPS URL

## Prerequisites

- Docker Desktop is running
- the project is opened at the repo root in Command Prompt
- `ngrok` is installed and already authenticated on this machine

Example auth command if you have not done it yet:

```cmd
ngrok config add-authtoken <your-token>
```

## 1. Start the local stack

From the project root:

```cmd
docker compose up -d --build
```

Optional check:

```cmd
docker compose ps
```

You want these services running before continuing:

- `frontend`
- `backend`
- `keycloak`

## 2. Confirm the local site works

Open:

- `http://localhost:3000`

If the local frontend is down, fix that first before starting `ngrok`.

## 3. Start one ngrok tunnel

Open a second Command Prompt window and run:

```cmd
ngrok http http://localhost:3000
```

Copy the HTTPS forwarding URL from `ngrok`.

Use the HTTPS URL, not the HTTP one.

## 4. Apply single-origin public-test mode

Back in the project root, run this from Command Prompt:

```cmd
scripts\start-public-test.cmd -PublicUrl https://<your-ngrok-url>
```

What this does:

- rewrites `.env.public-test`
- recreates `keycloak`, `backend`, and `frontend`
- makes the frontend serve the SPA and proxy `/api` plus `/auth`
- updates the Keycloak `library-web` client redirect URIs and web origins for the public URL

If you want to inspect the changes first:

```cmd
scripts\start-public-test.cmd -PublicUrl https://<your-ngrok-url> -WhatIf
```

## 5. Verify the public URL

Open the `ngrok` HTTPS URL in a browser and check:

1. the landing page loads
2. `/books` loads
3. login opens Keycloak under `/auth`
4. authenticated API calls work after login

If all four work, the setup is complete.

## 6. Share the URL

Share only the single `ngrok` HTTPS URL with testers.

They do not need separate backend or Keycloak URLs.

## Optional env-file workflow

If you prefer editing a file instead of passing the URL inline:

```cmd
copy scripts\public-test.env.example .env.public-test
notepad .env.public-test
scripts\start-public-test.cmd
```

Set:

- `PUBLIC_TEST_URL=https://<your-ngrok-url>`

## When the ngrok URL changes

Free `ngrok` URLs usually change when you restart the tunnel.

If that happens:

1. start the new tunnel again with `ngrok http http://localhost:3000`
2. copy the new HTTPS URL
3. rerun:

```cmd
scripts\start-public-test.cmd -PublicUrl https://<your-new-ngrok-url>
```

Do not skip the rerun. Keycloak redirect settings need the new public URL.

## Return to normal local mode

To switch back to the normal local Docker URLs:

```cmd
docker compose up -d --build keycloak backend frontend
```

## Troubleshooting

### `ngrok` is not recognized

Either:

- add `ngrok` to `PATH`
- or run it with its full file path

Example:

```cmd
C:\tools\ngrok\ngrok.exe http http://localhost:3000
```

### `localhost:3000` does not load

Run:

```cmd
docker compose up -d --build frontend backend keycloak
```

Then test `http://localhost:3000` again before starting `ngrok`.

### Login redirects fail after restarting ngrok

Your public URL changed. Rerun:

```cmd
scripts\start-public-test.cmd -PublicUrl https://<your-new-ngrok-url>
```

### You only have one free tunnel

That is fine. This setup is designed for one tunnel only.

Use only:

- `ngrok http http://localhost:3000`

Do not start extra tunnels for:

- backend `8080`
- Keycloak `8081`
