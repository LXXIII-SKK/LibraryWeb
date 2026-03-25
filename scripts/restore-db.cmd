@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "REPO_ROOT=%%~fI"

set "ENV_FILE="
set "INPUT_FILE="
set "FORCE=0"

:parse_args
if "%~1"=="" goto args_done
if /I "%~1"=="-EnvFile" (
  set "ENV_FILE=%~2"
  shift
  shift
  goto parse_args
)
if /I "%~1"=="-Force" (
  set "FORCE=1"
  shift
  goto parse_args
)
if defined INPUT_FILE (
  echo Unexpected argument: %~1
  exit /b 1
)
set "INPUT_FILE=%~1"
shift
goto parse_args

:args_done
if not defined INPUT_FILE (
  echo Usage: scripts\restore-db.cmd ^<backup-file.sql^> [-Force] [-EnvFile path]
  exit /b 1
)

for %%I in ("%INPUT_FILE%") do set "INPUT_FILE=%%~fI"
if not exist "%INPUT_FILE%" (
  echo Backup file "%INPUT_FILE%" was not found.
  exit /b 1
)

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

docker inspect "%POSTGRES_CONTAINER_NAME%" >nul 2>&1
if errorlevel 1 (
  echo PostgreSQL container "%POSTGRES_CONTAINER_NAME%" is not running.
  echo Start the stack first with: docker compose up -d postgres
  exit /b 1
)

if "%FORCE%"=="0" (
  echo Restore target: %POSTGRES_DB% in %POSTGRES_CONTAINER_NAME%
  echo Source dump: %INPUT_FILE%
  echo This will overwrite existing data in the target database.
  set /p CONFIRM=Type RESTORE to continue: 
  if /I not "%CONFIRM%"=="RESTORE" (
    echo Restore cancelled.
    exit /b 1
  )
)

docker exec -i -e PGPASSWORD=%POSTGRES_PASSWORD% "%POSTGRES_CONTAINER_NAME%" psql -v ON_ERROR_STOP=1 -U %POSTGRES_USER% -d %POSTGRES_DB% < "%INPUT_FILE%"
if errorlevel 1 (
  echo Restore failed.
  exit /b 1
)

echo Restore completed from %INPUT_FILE%
echo Recommended next steps:
echo   1. scripts\verify-migrations.cmd
echo   2. scripts\verify-runtime.cmd
exit /b 0
