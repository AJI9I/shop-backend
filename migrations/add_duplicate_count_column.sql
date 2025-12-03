-- Миграция: Добавление колонки duplicate_count в таблицу whatsapp_messages
-- Дата: 2025-12-01
-- Описание: Добавляет колонку для подсчета дубликатов сообщений
-- Примечание: Если используется Hibernate ddl-auto: update, эта миграция может не потребоваться

-- Для PostgreSQL
ALTER TABLE whatsapp_messages 
ADD COLUMN IF NOT EXISTS duplicate_count INTEGER NOT NULL DEFAULT 0;

-- Комментарий к колонке
COMMENT ON COLUMN whatsapp_messages.duplicate_count IS 
'Счетчик дубликатов - количество раз, когда от этого отправителя было получено идентичное сообщение в других группах за последние 10 минут';


