@echo off
echo ========================================
echo Остановка Java процессов и очистка
echo ========================================
echo.

echo Останавливаем процессы Java...
taskkill /F /IM java.exe 2>nul
taskkill /F /IM javaw.exe 2>nul
timeout /t 2 /nobreak >nul

echo.
echo Очищаем директорию target...
if exist target (
    rmdir /s /q target 2>nul
    if exist target (
        echo ВНИМАНИЕ: Не удалось полностью удалить директорию target
        echo Закройте IDE и все Java процессы вручную, затем попробуйте снова
        pause
        exit /b 1
    )
)

echo.
echo Выполняем очистку через Maven...
call mvn clean

echo.
echo Собираем проект...
call mvn compile -DskipTests

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
    pause
    exit /b 1
)

pause
