-- Миграция для добавления таблицы sellers и связи с offers
-- Выполняется при запуске приложения (если включен sql.init.mode)
-- Ошибки будут проигнорированы благодаря continue-on-error: true

-- Создаем таблицу sellers, если она не существует
CREATE TABLE IF NOT EXISTS sellers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    phone VARCHAR(50) NOT NULL UNIQUE,
    whatsapp_id VARCHAR(200),
    contact_info TEXT,
    rating DOUBLE,
    deals_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Добавляем столбец seller_id в таблицу offers
-- H2 не поддерживает IF NOT EXISTS для ALTER TABLE, ошибка будет проигнорирована
ALTER TABLE offers ADD seller_id BIGINT;

-- Добавляем столбец operation_type в таблицу offers
ALTER TABLE offers ADD operation_type VARCHAR(10) DEFAULT 'SELL';

-- Обновляем существующие записи: устанавливаем operation_type = 'SELL' по умолчанию
UPDATE offers SET operation_type = 'SELL' WHERE operation_type IS NULL;

