-- Миграция: Добавление колонки active в таблицу miner_details
-- Дата: 2025-11-30

-- Проверяем, существует ли колонка, и добавляем её, если её нет
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'miner_details' 
        AND column_name = 'active'
    ) THEN
        -- Добавляем колонку с значением по умолчанию
        ALTER TABLE miner_details 
        ADD COLUMN active BOOLEAN NOT NULL DEFAULT true;
        
        -- Обновляем все существующие записи (на всякий случай)
        UPDATE miner_details SET active = true WHERE active IS NULL;
        
        RAISE NOTICE 'Колонка active успешно добавлена в таблицу miner_details';
    ELSE
        RAISE NOTICE 'Колонка active уже существует в таблице miner_details';
    END IF;
END $$;




