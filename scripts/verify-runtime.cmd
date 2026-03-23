@echo off
setlocal

set MODE=%~1
set FRONTEND_URL=http://localhost:3000
if /I "%MODE%"=="vite" set FRONTEND_URL=http://localhost:5173

set BACKEND_URL=http://localhost:8080/actuator/health
set KEYCLOAK_URL=http://localhost:8081/realms/library/.well-known/openid-configuration

call :check "%FRONTEND_URL%" "frontend"
call :check "%BACKEND_URL%" "backend"
call :check "%KEYCLOAK_URL%" "keycloak"

echo Runtime verification passed.
echo Frontend: %FRONTEND_URL%
echo Backend:  %BACKEND_URL%
echo Keycloak: %KEYCLOAK_URL%
exit /b 0

:check
curl -fsS %~1 >nul
if errorlevel 1 (
  echo Runtime verification failed for %~2 at %~1
  exit /b 1
)
exit /b 0
