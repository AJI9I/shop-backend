@echo off
chcp 65001 >nul
echo Stopping Java processes...
taskkill /F /IM java.exe 2>nul
timeout /t 2 /nobreak >nul

echo Cleaning project...
call mvn clean -f pom.xml

echo Building project...
call mvn package -DskipTests -f pom.xml

echo Starting application...
start "Shop Backend" cmd /k "mvn spring-boot:run -f pom.xml"


