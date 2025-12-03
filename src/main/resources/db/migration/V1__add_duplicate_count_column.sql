-- Миграция: Добавление колонки duplicate_count в таблицу whatsapp_messages
-- Выполняется автоматически при старте Spring Boot через spring.sql.init

-- Проверяем, существует ли колонка, и добавляем её, если её нет
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'whatsapp_messages' 
        AND column_name = 'duplicate_count'
    ) THEN
        -- Добавляем колонку с возможностью NULL (временно)
        ALTER TABLE whatsapp_messages 
        ADD COLUMN duplicate_count INTEGER;
        
        -- Обновляем все существующие записи, устанавливая 0
        UPDATE whatsapp_messages 
        SET duplicate_count = 0 
        WHERE duplicate_count IS NULL;
        
        -- Делаем колонку NOT NULL с DEFAULT 0
        ALTER TABLE whatsapp_messages 
        ALTER COLUMN duplicate_count SET NOT NULL,
        ALTER COLUMN duplicate_count SET DEFAULT 0;
        
        RAISE NOTICE 'Колонка duplicate_count успешно добавлена в таблицу whatsapp_messages';
    ELSE
        -- Колонка уже существует, проверяем, может ли быть NULL
        IF EXISTS (
            SELECT 1 
            FROM information_schema.columns 
            WHERE table_name = 'whatsapp_messages' 
            AND column_name = 'duplicate_count'
            AND is_nullable = 'YES'
        ) THEN
            -- Обновляем NULL значения
            UPDATE whatsapp_messages 
            SET duplicate_count = 0 
            WHERE duplicate_count IS NULL;
            
            -- Устанавливаем NOT NULL и DEFAULT
            ALTER TABLE whatsapp_messages 
            ALTER COLUMN duplicate_count SET NOT NULL,
            ALTER COLUMN duplicate_count SET DEFAULT 0;
            
            RAISE NOTICE 'Колонка duplicate_count исправлена: установлен NOT NULL и DEFAULT 0';
        END IF;
    END IF;
END $$;

