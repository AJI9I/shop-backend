@echo off
chcp 65001 >nul
echo ========================================
echo Остановка Java процессов
echo ========================================
taskkill /F /IM java.exe >nul 2>&1
taskkill /F /IM javaw.exe >nul 2>&1
timeout /t 3 /nobreak >nul

echo Очистка директории target...
if exist target (
    rmdir /s /q target 2>nul
)

echo Выполнение Maven clean и compile...
call mvn clean compile -DskipTests

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Сборка успешно завершена!
    echo ========================================
) else (
    echo.
    echo ========================================
    echo Ошибка при сборке!
    echo ========================================
)

pause




