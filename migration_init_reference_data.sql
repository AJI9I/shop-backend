-- Миграция для инициализации справочных данных
-- Валюты и единицы измерения хэшрейта
-- Выполняется автоматически при развертывании приложения
-- Можно выполнить вручную, если автоматическая инициализация не сработала

-- Инициализация валют (только если таблица пустая)
INSERT INTO currencies (code, name, symbol, is_base, display_order) 
SELECT 'RUB', 'Российский рубль', '₽', true, 1
WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'RUB');

INSERT INTO currencies (code, name, symbol, is_base, display_order) 
SELECT 'USD', 'Доллар США', '$', false, 2
WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'USD');

INSERT INTO currencies (code, name, symbol, is_base, display_order) 
SELECT 'EUR', 'Евро', '€', false, 3
WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'EUR');

INSERT INTO currencies (code, name, symbol, is_base, display_order) 
SELECT 'CNY', 'Китайский юань', '¥', false, 4
WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'CNY');

-- Инициализация единиц измерения хэшрейта (только если таблица пустая)
INSERT INTO hashrate_units (name, abbreviation, multiplier, display_order) 
SELECT 'Хеш в секунду', 'H/s', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM hashrate_units WHERE abbreviation = 'H/s');

INSERT INTO hashrate_units (name, abbreviation, multiplier, display_order) 
SELECT 'КилоХеш в секунду', 'KH/s', 1000, 2
WHERE NOT EXISTS (SELECT 1 FROM hashrate_units WHERE abbreviation = 'KH/s');

INSERT INTO hashrate_units (name, abbreviation, multiplier, display_order) 
SELECT 'МегаХеш в секунду', 'MH/s', 1000000, 3
WHERE NOT EXISTS (SELECT 1 FROM hashrate_units WHERE abbreviation = 'MH/s');

INSERT INTO hashrate_units (name, abbreviation, multiplier, display_order) 
SELECT 'ГигаХеш в секунду', 'GH/s', 1000000000, 4
WHERE NOT EXISTS (SELECT 1 FROM hashrate_units WHERE abbreviation = 'GH/s');

INSERT INTO hashrate_units (name, abbreviation, multiplier, display_order) 
SELECT 'ТераХеш в секунду', 'TH/s', 1000000000000, 5
WHERE NOT EXISTS (SELECT 1 FROM hashrate_units WHERE abbreviation = 'TH/s');

INSERT INTO hashrate_units (name, abbreviation, multiplier, display_order) 
SELECT 'ПетаХеш в секунду', 'PH/s', 1000000000000000, 6
WHERE NOT EXISTS (SELECT 1 FROM hashrate_units WHERE abbreviation = 'PH/s');

INSERT INTO hashrate_units (name, abbreviation, multiplier, display_order) 
SELECT 'ЭксаХеш в секунду', 'EH/s', 1000000000000000000, 7
WHERE NOT EXISTS (SELECT 1 FROM hashrate_units WHERE abbreviation = 'EH/s');

INSERT INTO hashrate_units (name, abbreviation, multiplier, display_order) 
SELECT 'КилоСол в секунду', 'kSol/s', 1000, 8
WHERE NOT EXISTS (SELECT 1 FROM hashrate_units WHERE abbreviation = 'kSol/s');

INSERT INTO hashrate_units (name, abbreviation, multiplier, display_order) 
SELECT 'МегаСол в секунду', 'MSol/s', 1000000, 9
WHERE NOT EXISTS (SELECT 1 FROM hashrate_units WHERE abbreviation = 'MSol/s');

INSERT INTO hashrate_units (name, abbreviation, multiplier, display_order) 
SELECT 'ГигаСол в секунду', 'GSol/s', 1000000000, 10
WHERE NOT EXISTS (SELECT 1 FROM hashrate_units WHERE abbreviation = 'GSol/s');


