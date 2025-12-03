@echo off
chcp 65001 >nul
echo ========================================
echo Принудительная очистка и сборка Spring Boot
echo ========================================
echo.

cd /d "%~dp0"

echo Шаг 1: Остановка всех Java процессов...
for /f "tokens=2" %%a in ('tasklist /FI "IMAGENAME eq java.exe" /FO LIST 2^>nul ^| findstr /C:"PID:"') do (
    echo   Остановка процесса PID: %%a
    taskkill /PID %%a /F >nul 2>&1
)

for /f "tokens=2" %%a in ('tasklist /FI "IMAGENAME eq javaw.exe" /FO LIST 2^>nul ^| findstr /C:"PID:"') do (
    echo   Остановка процесса PID: %%a
    taskkill /PID %%a /F >nul 2>&1
)

echo.
echo Шаг 2: Ожидание освобождения файлов (5 секунд)...
timeout /t 5 /nobreak >nul

echo.
echo Шаг 3: Попытка удаления директории target...
if exist "target" (
    echo   Удаление target...
    rd /s /q "target" 2>nul
    if exist "target" (
        echo   ВНИМАНИЕ: Не удалось удалить target автоматически
        echo   Попробуйте удалить вручную или закройте IDE/другие программы
        echo   Нажмите любую клавишу после ручного удаления...
        pause >nul
    ) else (
        echo   Директория target успешно удалена
    )
) else (
    echo   Директория target не существует
)

echo.
echo Шаг 4: Сборка проекта...
echo.

mvn clean install -DskipTests

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
    echo Возможные решения:
    echo 1. Закройте IDE (IntelliJ IDEA, Eclipse и т.д.)
    echo 2. Закройте все программы, которые могут использовать файлы из target
    echo 3. Удалите директорию target вручную
    echo 4. Перезапустите компьютер (если ничего не помогает)
)

echo.
pause

