@echo off
chcp 65001 >nul
title Shop Backend - Starting...
echo.
echo ================================================
echo   Miners Shop Backend - Restart Script
echo ================================================
echo.

REM Остановка Java процессов
echo [1/4] Stopping Java processes...
taskkill /F /IM java.exe >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo       Java processes stopped.
    timeout /t 2 /nobreak >nul
) else (
    echo       No Java processes found.
)
echo.

REM Переход в директорию скрипта
cd /d %~dp0
echo [2/4] Current directory: %CD%
echo.

REM Очистка проекта
echo [3/4] Cleaning Maven project...
call mvn clean >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo       Clean completed successfully.
) else (
    echo       Warning: Clean had issues (continuing anyway).
)
echo.

REM Сборка проекта
echo [4/4] Building Maven project...
call mvn package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Build failed!
    pause
    exit /b 1
)
echo.
echo ================================================
echo   Build completed successfully!
echo ================================================
echo.

REM Запуск приложения
echo Starting Spring Boot application...
echo Application will open in a new window.
echo.
start "Shop Backend Application" cmd /k "title Shop Backend - Running && mvn spring-boot:run"
timeout /t 3 /nobreak >nul

echo.
echo ================================================
echo   Application is starting...
echo   Check the new window for startup logs.
echo ================================================
echo.
pause


