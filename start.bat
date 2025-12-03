@echo off
chcp 65001 >nul
REM Устанавливаем кодировку UTF-8 для Maven и Java
set MAVEN_OPTS=-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8
cd /d %~dp0
taskkill /F /IM java.exe >nul 2>&1
timeout /t 2 /nobreak >nul
start "Shop Backend" cmd /k "title Shop Backend && chcp 65001 >nul && set MAVEN_OPTS=-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 && set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 && cd /d %~dp0 && mvn spring-boot:run"
