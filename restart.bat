@echo off
chcp 65001 >nul
title Shop Backend - Restarting...
cd /d %~dp0

echo.
echo ================================================
echo   Miners Shop Backend - Restart
echo ================================================
echo.

REM Остановка Java процессов
echo [1/3] Stopping Java processes...
taskkill /F /IM java.exe >nul 2>&1
timeout /t 2 /nobreak >nul
echo Done.
echo.

REM Перезапуск
echo [2/3] Starting Spring Boot application...
echo Application will open in a new window.
echo.
start "Shop Backend Application" cmd /k "title Shop Backend - Running && chcp 65001 >nul && cd /d %~dp0 && mvn spring-boot:run"
timeout /t 3 /nobreak >nul

echo.
echo [3/3] Application is starting...
echo Check the new window for startup logs.
echo Web interface: http://localhost:8080
echo.
echo ================================================
echo   Application restarted!
echo ================================================
echo.
pause
