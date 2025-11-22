-- SQL скрипт для экспорта данных из таблицы products
-- Выполнить в PostgreSQL для получения данных всех товаров

SELECT 
    id,
    model,
    manufacturer,
    description,
    created_at,
    updated_at,
    miner_detail_id
FROM products
ORDER BY id;

-- Экспорт в CSV (выполнить в psql):
-- \copy (SELECT id, model, manufacturer, description, created_at, updated_at, miner_detail_id FROM products ORDER BY id) TO 'products_data.csv' WITH CSV HEADER;

