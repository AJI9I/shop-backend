@echo off
chcp 65001 >nul
REM Скрипт для выполнения миграции базы данных
REM Добавляет колонку duplicate_count в таблицу whatsapp_messages

echo ========================================
echo Выполнение миграции базы данных
echo ========================================
echo.
echo Миграция: Добавление колонки duplicate_count в таблицу whatsapp_messages
echo.

REM Параметры подключения к базе данных (значения по умолчанию)
set DB_HOST=localhost
set DB_PORT=5432
set DB_NAME=miners
set DB_USER=postgres
set DB_PASSWORD=vasagaroot

REM Проверяем переменные окружения
if not "%SPRING_DATASOURCE_URL%"=="" (
    REM Извлекаем параметры из URL вида jdbc:postgresql://host:port/database
    for /f "tokens=3 delims=/" %%a in ("%SPRING_DATASOURCE_URL%") do set DB_URL_PART=%%a
    for /f "tokens=1 delims=:" %%a in ("%DB_URL_PART%") do set DB_HOST=%%a
    for /f "tokens=2 delims=:" %%a in ("%DB_URL_PART%") do set DB_PORT=%%a
    for /f "tokens=2 delims=/" %%a in ("%SPRING_DATASOURCE_URL%") do set DB_NAME=%%a
)
if not "%SPRING_DATASOURCE_USERNAME%"=="" set DB_USER=%SPRING_DATASOURCE_USERNAME%
if not "%SPRING_DATASOURCE_PASSWORD%"=="" set DB_PASSWORD=%SPRING_DATASOURCE_PASSWORD%

echo Параметры подключения:
echo   Host: %DB_HOST%
echo   Port: %DB_PORT%
echo   Database: %DB_NAME%
echo   User: %DB_USER%
echo.

REM Устанавливаем переменную окружения для пароля psql
set PGPASSWORD=%DB_PASSWORD%

REM Выполняем SQL через psql
echo Выполнение миграции через psql...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f migration_add_duplicate_count_column.sql
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Миграция успешно выполнена!
    echo ========================================
    goto :end
)

REM Если psql недоступен или произошла ошибка
echo.
echo ========================================
echo ВНИМАНИЕ: psql не найден или произошла ошибка
echo ========================================
echo.
echo Пожалуйста, выполните SQL вручную через любой PostgreSQL клиент:
echo.
echo 1. Подключитесь к базе данных:
echo    psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME%
echo.
echo 2. Выполните SQL из файла migration_add_duplicate_count_column.sql:
echo.
type migration_add_duplicate_count_column.sql

:end
echo.
pause


