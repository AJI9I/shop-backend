@echo off
REM Скрипт для выполнения миграции базы данных
REM Добавляет колонку active в таблицу miner_details

echo Выполнение миграции базы данных...
echo.

REM Параметры подключения к базе данных
set DB_URL=jdbc:postgresql://localhost:5432/miners
set DB_USER=postgres
set DB_PASSWORD=vasagaroot

REM Проверяем, доступна ли переменная окружения
if not "%SPRING_DATASOURCE_URL%"=="" set DB_URL=%SPRING_DATASOURCE_URL%
if not "%SPRING_DATASOURCE_USERNAME%"=="" set DB_USER=%SPRING_DATASOURCE_USERNAME%
if not "%SPRING_DATASOURCE_PASSWORD%"=="" set DB_PASSWORD=%SPRING_DATASOURCE_PASSWORD%

echo URL: %DB_URL%
echo User: %DB_USER%
echo.

REM Выполняем SQL через psql, если доступен
psql -h localhost -U %DB_USER% -d miners -f migration_add_active_column.sql 2>nul
if %ERRORLEVEL% EQU 0 (
    echo Миграция успешно выполнена через psql!
    goto :end
)

REM Если psql недоступен, выводим инструкцию
echo.
echo ВНИМАНИЕ: psql не найден или миграция не выполнена автоматически.
echo.
echo Пожалуйста, выполните SQL вручную через любой PostgreSQL клиент:
echo.
echo 1. Подключитесь к базе данных:
echo    psql -h localhost -U postgres -d miners
echo.
echo 2. Выполните SQL из файла migration_add_active_column.sql:
echo.
type migration_add_active_column.sql

:end
pause




