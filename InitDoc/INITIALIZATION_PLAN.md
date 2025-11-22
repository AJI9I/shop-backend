# План инициализации MinerDetail для существующих Product

## Цель
Создать MinerDetail для всех существующих товаров (Product) в базе данных, которые являются майнерами.

---

## Шаг 1: Получение данных из таблицы products

### Задача
Сделать запрос к таблице `products` и сохранить результаты в документ.

### Действия:
1. **Подключиться к базе данных PostgreSQL**
   - База: `miners`
   - Хост: `localhost:5432`
   - Пользователь: `postgres`
   - Пароль: `vasagaroot`

2. **Выполнить SQL запрос:**
   ```sql
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
   ```

3. **Сохранить результаты в файл:**
   - Путь: `shop-backend/InitDoc/products_data.csv` или `products_data.json`
   - Формат: CSV (для удобства анализа) или JSON (для структурированных данных)

### Ожидаемый результат:
Файл с данными всех товаров из таблицы `products`.

---

## Шаг 2: Анализ данных на совпадения

### Задача
Проверить полученную информацию на:
- Дубликаты (несколько Product с одинаковым или похожим названием)
- Товары, которые уже ссылаются на MinerDetail (miner_detail_id != NULL)
- Товары без MinerDetail (miner_detail_id IS NULL)

### Действия:
1. **Проанализировать сохраненные данные:**
   - Найти товары с одинаковым или похожим `model`
   - Найти товары, у которых уже есть `miner_detail_id`
   - Найти товары без `miner_detail_id`

2. **Создать отчет о найденных совпадениях:**
   - Файл: `shop-backend/InitDoc/analysis_report.md`
   - Содержание:
     - Список дубликатов (товары с похожими названиями)
     - Список товаров с существующими MinerDetail
     - Список товаров без MinerDetail (требуют инициализации)

### Ожидаемый результат:
Отчет с анализом данных и списком товаров, требующих инициализации.

---

## Шаг 3: Определение майнеров

### Задача
Определить все полученные продукты, которые принадлежат к майнерам (ASIC майнеры).

### Критерии определения майнера:
- Название содержит типичные модели майнеров (S19, S21, L7, M50, и т.д.)
- Производитель из списка: Bitmain, MicroBT, Canaan, Avalon, Innosilicon, Goldshell, и т.д.
- Исключить: платы, кабели, аксессуары (если есть в данных)

### Действия:
1. **Создать список майнеров:**
   - Файл: `shop-backend/InitDoc/miners_list.json`
   - Формат JSON с данными каждого майнера:
   ```json
   [
     {
       "productId": 1,
       "model": "S19j PRO 104T",
       "manufacturer": "Bitmain",
       "needsInitialization": true,
       "notes": "Требует инициализации"
     },
     {
       "productId": 2,
       "model": "S19j PRO 104 TH/s",
       "manufacturer": "Bitmain",
       "needsInitialization": true,
       "notes": "Похож на Product ID=1, возможно объединение"
     }
   ]
   ```

2. **Пометить товары:**
   - Майнеры (требуют инициализации)
   - Не майнеры (платы, кабели - пропускаем)
   - Уже имеют MinerDetail (пропускаем)

### Ожидаемый результат:
Список всех майнеров, которые требуют инициализации.

---

## Шаг 4: Создание параметров инициализации для каждого майнера

### Задача
Для каждого майнера из списка создать параметры инициализации с известными данными.

### Действия:
1. **Создать файл с параметрами инициализации:**
   - Файл: `shop-backend/InitDoc/initialization_params.json`
   - Формат:
   ```json
   [
     {
       "productId": 1,
       "productModel": "S19j PRO 104T",
       "minerDetail": {
         "standardName": "Antminer S19j PRO 104T",
         "manufacturer": "Bitmain",
         "series": "S19j",
         "hashrate": null,
         "algorithm": null,
         "powerConsumption": null,
         "coins": null,
         "powerSource": null,
         "cooling": null,
         "operatingTemperature": null,
         "dimensions": null,
         "noiseLevel": null,
         "description": null,
         "features": null,
         "placementInfo": null,
         "producerInfo": null
       },
       "needsAdditionalInfo": true,
       "notes": "Требуется дополнительная информация через curl запросы"
     }
   ]
   ```

2. **Заполнить известные данные:**
   - `standardName` - стандартизированное название (можно взять из model или задать вручную)
   - `manufacturer` - из Product.manufacturer
   - `series` - извлечь из model (S19j, L7, S21, и т.д.)

3. **Пометить поля, требующие дополнительной информации:**
   - `needsAdditionalInfo: true` - если нужны запросы через curl для получения данных о майнере

### Ожидаемый результат:
Файл с параметрами инициализации для каждого майнера.

---

## Шаг 5: Получение дополнительной информации через curl (опционально)

### Задача
Для майнеров, у которых не хватает данных, сделать запросы через curl для получения информации.

### Действия:
1. **Определить майнеры, требующие дополнительной информации:**
   - Из файла `initialization_params.json` найти записи с `needsAdditionalInfo: true`

2. **Подготовить curl запросы:**
   - Файл: `shop-backend/InitDoc/curl_requests.sh` или `curl_requests.md`
   - Примеры запросов для получения информации о майнере (если есть внешний API или веб-источник)

3. **Выполнить запросы (пользователь выполнит с своего компьютера):**
   ```bash
   # Пример (если есть API для получения информации о майнере)
   curl -X GET "https://api.example.com/miners/S19j-PRO-104T" \
        -H "Accept: application/json" \
        > shop-backend/InitDoc/miner_info_S19j_PRO_104T.json
   ```

4. **Сохранить полученные данные:**
   - Файлы: `shop-backend/InitDoc/miner_info_<model>.json`
   - Обновить `initialization_params.json` с полученными данными

### Ожидаемый результат:
Дополнительная информация о майнерах, если требуется.

---

## Шаг 6: Выполнение инициализации

### Задача
Создать MinerDetail для каждого майнера из списка с подготовленными параметрами.

### Действия:
1. **Создать скрипт инициализации:**
   - Файл: `shop-backend/InitDoc/initialize_miners.sql` или Java-метод
   - Вариант 1: SQL скрипт для прямого создания записей
   - Вариант 2: Использовать существующий API endpoint `/api/init/miner-details`

2. **Выполнить инициализацию:**
   - Вариант A: Автоматически через API (создает MinerDetail для всех Product без MinerDetail)
     ```bash
     curl -X POST http://localhost:8080/api/init/miner-details
     ```
   - Вариант B: Вручную для каждого майнера с подготовленными параметрами

3. **Проверить результаты:**
   - Запрос к БД: `SELECT COUNT(*) FROM miner_details;`
   - Сравнить с количеством майнеров из списка

### Ожидаемый результат:
Все майнеры имеют связанные MinerDetail записи.

---

## Шаг 7: Контроль и верификация

### Задача
Проверить корректность созданных MinerDetail записей.

### Действия:
1. **Проверить созданные записи:**
   ```sql
   SELECT 
       md.id,
       md.standard_name,
       md.manufacturer,
       md.series,
       COUNT(p.id) as products_count
   FROM miner_details md
   LEFT JOIN products p ON p.miner_detail_id = md.id
   GROUP BY md.id, md.standard_name, md.manufacturer, md.series
   ORDER BY md.id;
   ```

2. **Проверить связь Product -> MinerDetail:**
   ```sql
   SELECT 
       p.id as product_id,
       p.model,
       p.miner_detail_id,
       md.standard_name
   FROM products p
   LEFT JOIN miner_details md ON md.id = p.miner_detail_id
   WHERE p.miner_detail_id IS NOT NULL
   ORDER BY p.id;
   ```

3. **Создать отчет о результатах:**
   - Файл: `shop-backend/InitDoc/verification_report.md`
   - Содержание:
     - Количество созданных MinerDetail
     - Количество Product, связанных с MinerDetail
     - Список проблем (если есть)

### Ожидаемый результат:
Отчет о корректности инициализации.

---

## Структура папки InitDoc

```
shop-backend/InitDoc/
├── INITIALIZATION_PLAN.md          # Этот файл (план)
├── products_data.csv               # Данные из таблицы products
├── analysis_report.md              # Анализ данных на совпадения
├── miners_list.json                # Список майнеров для инициализации
├── initialization_params.json       # Параметры инициализации для каждого майнера
├── curl_requests.sh                 # Скрипт с curl запросами (если нужны)
├── miner_info_*.json                # Дополнительная информация о майнерах (если получена)
└── verification_report.md           # Отчет о результатах инициализации
```

---

## Важные замечания

1. **Резервное копирование:**
   - Перед инициализацией сделать бэкап базы данных
   - Сохранить текущее состояние таблиц `products` и `miner_details`

2. **Транзакционность:**
   - Инициализация должна быть атомарной (все или ничего)
   - Использовать транзакции при создании записей

3. **Логирование:**
   - Все действия должны логироваться
   - Сохранять логи в `shop-backend/InitDoc/init_logs.txt`

4. **Обработка ошибок:**
   - Если ошибка при создании MinerDetail для одного товара - не прерывать процесс
   - Записать ошибку в лог и продолжить

5. **Валидация данных:**
   - Проверять корректность данных перед созданием MinerDetail
   - Валидировать обязательные поля (standardName)

---

## Порядок выполнения

1. ✅ Шаг 1: Получить данные из products
2. ✅ Шаг 2: Проанализировать на совпадения
3. ✅ Шаг 3: Определить майнеры
4. ✅ Шаг 4: Создать параметры инициализации
5. ⏸️ Шаг 5: Получить дополнительную информацию (если нужно)
6. ⏸️ Шаг 6: Выполнить инициализацию
7. ⏸️ Шаг 7: Проверить результаты

---

*Документ создан: 2024-01-15*
*Статус: Подготовка к инициализации*

