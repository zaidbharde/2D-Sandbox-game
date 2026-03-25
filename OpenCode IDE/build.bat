@echo off
echo ==========================================
echo     OpenCode IDE - Build Script
echo ==========================================
echo.

cd /d "%~dp0"

echo [1/2] Creating directories...
if not exist "build\classes" mkdir build\classes
if not exist "dist" mkdir dist

echo [2/2] Compiling Java files...
javac -d build\classes -sourcepath src -encoding UTF-8 src\ide\*.java

if %errorlevel% neq 0 (
    echo.
    echo Build failed!
    pause
    exit /b 1
)

echo.
echo ==========================================
echo     Build successful!
echo ==========================================
echo.
echo To run the IDE, use: run.bat
echo.
pause
