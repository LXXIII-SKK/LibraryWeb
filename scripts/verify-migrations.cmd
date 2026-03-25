@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "REPO_ROOT=%%~fI"

set "ENV_FILE="

:parse_args
if "%~1"=="" goto args_done
if /I "%~1"=="-EnvFile" (
  set "ENV_FILE=%~2"
  shift
  shift
  goto parse_args
)
echo Unexpected argument: %~1
echo Usage: scripts\verify-migrations.cmd [-EnvFile path]
exit /b 1

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

docker inspect "%POSTGRES_CONTAINER_NAME%" >nul 2>&1
if errorlevel 1 (
  echo PostgreSQL container "%POSTGRES_CONTAINER_NAME%" is not running.
  echo Start the stack first with: docker compose up -d postgres backend
  exit /b 1
)

set "LATEST_VERSION=0"
pushd "%REPO_ROOT%\src\main\resources\db\migration" >nul
for %%F in (V*__*.sql) do (
  call :update_latest_version "%%~nF"
)
popd >nul

if "%LATEST_VERSION%"=="0" (
  echo No versioned Flyway migrations were found under src\main\resources\db\migration.
  exit /b 1
)

call :read_query_result "select count(*) from information_schema.tables where table_schema = 'public' and table_name = 'flyway_schema_history';" HISTORY_TABLE || exit /b 1
if not "%HISTORY_TABLE%"=="1" (
  echo flyway_schema_history was not found in %POSTGRES_DB%.
  echo Start the backend once so Flyway can apply the schema before running this check.
  exit /b 1
)

call :read_query_result "select version from flyway_schema_history where success and version is not null order by installed_rank desc limit 1;" CURRENT_VERSION || exit /b 1
call :read_query_result "select count(*) from flyway_schema_history where not success;" FAILED_COUNT || exit /b 1

if not defined CURRENT_VERSION (
  echo No applied Flyway version was found in flyway_schema_history.
  exit /b 1
)

if not "%FAILED_COUNT%"=="0" (
  echo Flyway has %FAILED_COUNT% failed migration record^(s^).
  exit /b 1
)

if not "%CURRENT_VERSION%"=="%LATEST_VERSION%" (
  echo Migration verification failed.
  echo Latest migration file version: %LATEST_VERSION%
  echo Database schema version:      %CURRENT_VERSION%
  exit /b 1
)

echo Migration verification passed.
echo Latest migration file version: %LATEST_VERSION%
echo Database schema version:      %CURRENT_VERSION%
echo Env file: %LOADED_COMPOSE_ENV_FILE%
exit /b 0

:update_latest_version
setlocal EnableDelayedExpansion
set "NAME=%~1"
for /f "tokens=1 delims=_" %%V in ("!NAME!") do set "VERSION=%%V"
set "VERSION=!VERSION:V=!"
if !VERSION! GTR %LATEST_VERSION% (
  endlocal & set "LATEST_VERSION=%VERSION%" & exit /b 0
)
endlocal
exit /b 0

:read_query_result
setlocal
set "SQL=%~1"
set "RESULT_FILE=%TEMP%\library-migration-%RANDOM%%RANDOM%.txt"
docker exec -e PGPASSWORD=%POSTGRES_PASSWORD% "%POSTGRES_CONTAINER_NAME%" psql -U %POSTGRES_USER% -d %POSTGRES_DB% -tAc "%SQL%" > "%RESULT_FILE%"
if errorlevel 1 (
  del /q "%RESULT_FILE%" >nul 2>&1
  echo Migration verification query failed.
  endlocal
  exit /b 1
)
set /p QUERY_RESULT=<"%RESULT_FILE%"
del /q "%RESULT_FILE%" >nul 2>&1
endlocal & set "%~2=%QUERY_RESULT%"
exit /b 0
