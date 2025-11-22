@echo off
echo Останавливаем старые процессы Java...
taskkill /F /IM java.exe 2>nul
timeout /t 3 /nobreak >nul
echo Запускаем приложение...
cd /d %~dp0
call mvn spring-boot:run
pause

