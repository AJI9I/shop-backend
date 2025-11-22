@echo off
chcp 65001 >nul
echo ========================================
echo Stopping Java processes...
echo ========================================
taskkill /F /IM java.exe 2>nul
if %ERRORLEVEL% EQU 0 (
    echo Java processes stopped.
    timeout /t 2 /nobreak >nul
) else (
    echo No Java processes found.
)
echo.

echo ========================================
echo Cleaning project...
echo ========================================
call mvn clean -f pom.xml
echo.

echo ========================================
echo Building project...
echo ========================================
call mvn package -DskipTests -f pom.xml
if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    pause
    exit /b 1
)
echo.

echo ========================================
echo Starting application...
echo ========================================
start "Shop Backend Application" cmd /k "mvn spring-boot:run -f pom.xml"
echo.
echo Application is starting in a new window...
echo Please wait for the application to start...


