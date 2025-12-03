# Миграция базы данных: Добавление колонки active

## Проблема
В таблице `miner_details` отсутствует колонка `active`, что не позволяет запустить приложение.

## Решение

### Вариант 1: Выполнить SQL напрямую (РЕКОМЕНДУЕТСЯ)

Выполните следующий SQL через любой PostgreSQL клиент (psql, pgAdmin, DBeaver и т.д.):

```sql
ALTER TABLE miner_details 
ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT true;
```

Или используйте полный скрипт из файла `migration_add_active_column.sql`.

### Вариант 2: Выполнить через psql

```bash
psql -h localhost -U postgres -d miners -f migration_add_active_column.sql
```

Или подключитесь к базе и выполните SQL вручную:

```bash
psql -h localhost -U postgres -d miners
```

Затем выполните:
```sql
ALTER TABLE miner_details ADD COLUMN active BOOLEAN NOT NULL DEFAULT true;
```

### Вариант 3: Автоматическая миграция при следующем запуске

После выполнения SQL миграции вручную, компонент `DatabaseMigrationConfig` будет проверять наличие колонки при каждом запуске приложения и добавлять её автоматически, если её нет.

## Проверка

После выполнения миграции проверьте, что колонка добавлена:

```sql
SELECT column_name, data_type, is_nullable, column_default 
FROM information_schema.columns 
WHERE table_name = 'miner_details' AND column_name = 'active';
```

Должен быть результат:
- `column_name`: `active`
- `data_type`: `boolean`
- `is_nullable`: `NO`
- `column_default`: `true`

## После миграции

После успешной миграции перезапустите Spring Boot приложение. Оно должно запуститься без ошибок.




