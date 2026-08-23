@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\stop-redis.ps1"
if errorlevel 1 (
    echo.
    echo Redis failed to stop. See the error above.
    pause
    exit /b 1
)
exit /b 0
