@echo off
chcp 65001 >nul
echo ========================================
echo Сборка Spring Boot без clean
echo ========================================
echo.
echo Выполняется сборка без очистки target...
echo.

cd /d "%~dp0"

mvn compile package -DskipTests

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Сборка успешно завершена!
    echo ========================================
) else (
    echo.
    echo ========================================
    echo ОШИБКА: Сборка не удалась!
    echo ========================================
    echo.
    echo Попробуйте:
    echo 1. Остановить Spring Boot приложение (запустите stop-spring-boot.bat)
    echo 2. Затем выполнить: mvn clean install
)

echo.
pause

