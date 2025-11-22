@echo off
cd /d %~dp0
echo Starting Spring Boot application...
call mvn spring-boot:run
pause

