@echo off
setlocal

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-public-test.ps1" %*
exit /b %errorlevel%
