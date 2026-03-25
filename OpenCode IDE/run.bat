@echo off
echo ==========================================
echo     OpenCode IDE
echo ==========================================
echo.

cd /d "%~dp0"

if not exist "build\classes\ide" (
    echo Project not built. Running build.bat first...
    call build.bat
    if %errorlevel% neq 0 exit /b 1
)

echo Starting OpenCode IDE...
echo.
java -cp build\classes ide.OpenCodeIDE

if %errorlevel% neq 0 (
    echo.
    echo Error running the application.
    pause
)
