@echo off
chcp 65001 >nul
echo ========================================
echo Пересборка и перезапуск Spring Boot
echo ========================================
echo.

REM Переходим в директорию скрипта
cd /d "%~dp0"

REM Проверяем и останавливаем только Spring Boot процессы по порту 8050
echo Проверка запущенных процессов Spring Boot...
netstat -ano | findstr ":8050" >nul
if %errorlevel% == 0 (
    echo Найден процесс на порту 8050, остановка...
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8050" ^| findstr "LISTENING"') do (
        echo Остановка процесса с PID: %%a
        taskkill /F /PID %%a >nul 2>&1
    )
    timeout /t 2 /nobreak >nul
    echo Spring Boot остановлен
) else (
    echo Spring Boot не запущен
)

echo.
echo Пересборка Spring Boot приложения...
call mvn clean package -DskipTests
if errorlevel 1 (
    echo ERROR: Сборка Spring Boot не удалась!
    pause
    exit /b 1
)

echo.
echo Запуск Spring Boot приложения...
start "Spring Boot - Логи" cmd /k "mvn spring-boot:run"

timeout /t 3 /nobreak >nul

echo.
echo ========================================
echo Spring Boot запущен!
echo ========================================
echo.
echo Веб-интерфейс: http://localhost:8050
echo Логи выводятся в отдельном окне CMD
echo.
timeout /t 2 /nobreak >nul

