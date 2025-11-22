@echo off
chcp 65001 >nul
cd /d %~dp0
taskkill /F /IM java.exe >nul 2>&1
timeout /t 2 /nobreak >nul
start "Shop Backend" cmd /k "title Shop Backend && chcp 65001 >nul && cd /d %~dp0 && mvn spring-boot:run"
