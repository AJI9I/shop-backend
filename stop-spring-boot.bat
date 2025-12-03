@echo off
chcp 65001 >nul
echo ========================================
echo Остановка Spring Boot приложения
echo ========================================
echo.

echo Поиск Java процессов...
for /f "tokens=2" %%a in ('tasklist /FI "IMAGENAME eq java.exe" /FO LIST ^| findstr /C:"PID:"') do (
    echo Найден Java процесс с PID: %%a
    echo Остановка процесса...
    taskkill /PID %%a /F
    if %ERRORLEVEL% EQU 0 (
        echo Процесс %%a успешно остановлен
    ) else (
        echo Не удалось остановить процесс %%a
    )
)

echo.
echo Проверка оставшихся Java процессов...
tasklist /FI "IMAGENAME eq java.exe" /FO LIST | findstr /C:"PID:" >nul
if %ERRORLEVEL% EQU 0 (
    echo ВНИМАНИЕ: Обнаружены оставшиеся Java процессы!
    echo Возможно, требуется остановить их вручную через Диспетчер задач
) else (
    echo Все Java процессы остановлены
)

echo.
echo ========================================
pause

