@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "REPO_ROOT=%%~fI"

set "ENV_FILE="
set "OUTPUT_FILE="

:parse_args
if "%~1"=="" goto args_done
if /I "%~1"=="-EnvFile" (
  set "ENV_FILE=%~2"
  shift
  shift
  goto parse_args
)
if defined OUTPUT_FILE (
  echo Unexpected argument: %~1
  exit /b 1
)
set "OUTPUT_FILE=%~1"
shift
goto parse_args

:args_done
call "%SCRIPT_DIR%load-compose-env.cmd" "%ENV_FILE%" || exit /b 1

if not defined POSTGRES_DB (
  echo POSTGRES_DB is not set in %LOADED_COMPOSE_ENV_FILE%.
  exit /b 1
)
if not defined POSTGRES_USER (
  echo POSTGRES_USER is not set in %LOADED_COMPOSE_ENV_FILE%.
  exit /b 1
)
if not defined POSTGRES_PASSWORD (
  echo POSTGRES_PASSWORD is not set in %LOADED_COMPOSE_ENV_FILE%.
  exit /b 1
)

set "POSTGRES_CONTAINER_NAME=mini-library-postgres"

if not defined OUTPUT_FILE (
  for /f %%I in ('powershell -NoProfile -Command "(Get-Date).ToString('yyyyMMdd-HHmmss')"') do set "STAMP=%%I"
  set "OUTPUT_FILE=%REPO_ROOT%\backups\library-%STAMP%.sql"
)

for %%I in ("%OUTPUT_FILE%") do (
  set "OUTPUT_FILE=%%~fI"
  set "OUTPUT_DIR=%%~dpI"
)

if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

docker inspect "%POSTGRES_CONTAINER_NAME%" >nul 2>&1
if errorlevel 1 (
  echo PostgreSQL container "%POSTGRES_CONTAINER_NAME%" is not running.
  echo Start the stack first with: docker compose up -d postgres
  exit /b 1
)

docker exec -e PGPASSWORD=%POSTGRES_PASSWORD% "%POSTGRES_CONTAINER_NAME%" pg_dump -U %POSTGRES_USER% -d %POSTGRES_DB% --clean --if-exists --no-owner --no-privileges > "%OUTPUT_FILE%"
if errorlevel 1 (
  del /q "%OUTPUT_FILE%" >nul 2>&1
  echo Backup failed.
  exit /b 1
)

echo Backup written to %OUTPUT_FILE%
echo Env file: %LOADED_COMPOSE_ENV_FILE%
exit /b 0
