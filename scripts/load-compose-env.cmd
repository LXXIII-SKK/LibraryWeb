@echo off
set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "REPO_ROOT=%%~fI"

set "ENV_FILE=%~1"
if not defined ENV_FILE set "ENV_FILE=%REPO_ROOT%\.env"
if not exist "%ENV_FILE%" set "ENV_FILE=%REPO_ROOT%\compose.env.example"

if not exist "%ENV_FILE%" (
  echo Could not find an env file. Expected "%REPO_ROOT%\.env" or "%REPO_ROOT%\compose.env.example".
  exit /b 1
)

for /f "usebackq eol=# tokens=1* delims==" %%A in ("%ENV_FILE%") do (
  if not "%%~A"=="" set "%%~A=%%~B"
)

set "LOADED_COMPOSE_ENV_FILE=%ENV_FILE%"
exit /b 0
