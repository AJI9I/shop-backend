-- Простая миграция: Добавление колонки active
-- Выполните этот SQL через любой PostgreSQL клиент

ALTER TABLE miner_details 
ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT true;




