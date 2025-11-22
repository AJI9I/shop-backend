@echo off
echo ============================================
echo Stopping Java processes...
echo ============================================
taskkill /F /IM java.exe 2>nul
timeout /t 2 /nobreak >nul

echo ============================================
echo Changing to shop-backend directory...
echo ============================================
cd /d %~dp0

echo ============================================
echo Cleaning Maven project...
echo ============================================
call mvn clean
if errorlevel 1 (
    echo ERROR: Clean failed!
    pause
    exit /b 1
)

echo ============================================
echo Building Maven project...
echo ============================================
call mvn package -DskipTests
if errorlevel 1 (
    echo ERROR: Build failed!
    pause
    exit /b 1
)

echo ============================================
echo Starting Spring Boot application...
echo ============================================
start "Shop Backend" cmd /k "mvn spring-boot:run"
echo.
echo Application is starting in a new window.
echo Please wait for the application to start.
echo.


