# Документация проекта Shop Backend

## Оглавление

1. [Общее описание](#общее-описание)
2. [Архитектура проекта](#архитектура-проекта)
3. [Структура базы данных](#структура-базы-данных)
4. [Контроллеры и страницы](#контроллеры-и-страницы)
5. [API Endpoints](#api-endpoints)
6. [Сервисы и бизнес-логика](#сервисы-и-бизнес-логика)
7. [Связи между компонентами](#связи-между-компонентами)
8. [Потоки данных](#потоки-данных)

---

## Общее описание

**Shop Backend** — это Spring Boot приложение для интернет-магазина майнеров с автоматическим обновлением ассортимента на основе сообщений из WhatsApp групп.

### Основные возможности:
- Автоматический парсинг сообщений из WhatsApp с помощью LLM (Ollama)
- Создание и обновление товаров (майнеров) с предложениями от продавцов
- Просмотр товаров в виде каталога и таблицы
- Фильтрация и сортировка предложений
- Управление заявками клиентов
- Просмотр сообщений WhatsApp и обработанных данных

### Технологический стек:
- **Java**: 23
- **Spring Boot**: 3.3.5
- **Spring Data JPA** + Hibernate
- **База данных**: PostgreSQL 16.4 (production) / H2 (development)
- **Frontend**: Thymeleaf + Bootstrap 5 + jQuery
- **Сборка**: Maven

---

## Архитектура проекта

### Структура директорий:

```
shop-backend/
├── src/main/java/com/miners/shop/
│   ├── ShopApplication.java              # Главный класс приложения
│   ├── controller/                       # Контроллеры (MVC и REST API)
│   │   ├── HomeController.java          # Главная страница
│   │   ├── ProductsController.java      # Страницы товаров
│   │   ├── MessagesController.java      # Просмотр сообщений
│   │   ├── RequestController.java       # Управление заявками
│   │   ├── WebhookController.java       # REST API для WhatsApp сервиса
│   │   └── AssetsController.java        # Статические ресурсы
│   ├── entity/                          # JPA сущности
│   │   ├── Product.java                 # Товар (майнер)
│   │   ├── Offer.java                   # Предложение о продаже/покупке
│   │   ├── Seller.java                  # Продавец
│   │   ├── WhatsAppMessage.java         # Сообщение WhatsApp
│   │   ├── Request.java                 # Заявка клиента
│   │   └── OperationType.java           # Тип операции (SELL/BUY)
│   ├── repository/                      # Spring Data JPA репозитории
│   │   ├── ProductRepository.java
│   │   ├── OfferRepository.java
│   │   ├── SellerRepository.java
│   │   ├── WhatsAppMessageRepository.java
│   │   └── RequestRepository.java
│   ├── service/                         # Бизнес-логика
│   │   ├── ProductService.java          # Работа с товарами и предложениями
│   │   ├── SellerService.java           # Работа с продавцами
│   │   ├── WhatsAppMessageService.java  # Работа с сообщениями
│   │   └── RequestService.java          # Работа с заявками
│   ├── dto/                             # Data Transfer Objects
│   │   ├── WhatsAppMessageDTO.java
│   │   ├── OfferDTO.java
│   │   └── RequestDTO.java
│   ├── config/                          # Конфигурация
│   │   ├── WebConfig.java
│   │   ├── EncodingConfig.java
│   │   └── AssetsCopyUtil.java
│   └── util/                            # Утилиты
│       └── ImageUrlResolver.java        # Разрешение URL изображений
├── src/main/resources/
│   ├── application.yml                  # Конфигурация приложения
│   ├── templates/                       # Thymeleaf шаблоны
│   │   ├── index-new.html              # Главная страница
│   │   ├── products-new.html           # Каталог товаров
│   │   ├── products-table.html         # Таблица товаров
│   │   ├── product-details-new.html    # Детальная страница товара
│   │   ├── messages.html               # Список сообщений
│   │   ├── message-view.html           # Просмотр сообщения
│   │   ├── requests.html               # Список заявок
│   │   └── request-details.html        # Детальная страница заявки
│   └── static/                         # Статические ресурсы (CSS, JS, изображения)
└── pom.xml                              # Maven конфигурация
```

---

## Структура базы данных

### Сущности и их связи:

#### 1. **Product** (Товар)
Таблица: `products`

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | BIGINT | Первичный ключ |
| `model` | VARCHAR(200) | Модель майнера (уникальный, например: "S19j PRO 104T") |
| `description` | TEXT | Описание товара (опционально) |
| `manufacturer` | VARCHAR(100) | Производитель (Bitmain, MicroBT, Canaan, и т.д.) |
| `created_at` | TIMESTAMP | Время создания |
| `updated_at` | TIMESTAMP | Время последнего обновления |

**Связи:**
- `OneToMany` → `Offer` (один товар может иметь много предложений)

#### 2. **Offer** (Предложение)
Таблица: `offers`

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | BIGINT | Первичный ключ |
| `product_id` | BIGINT | Ссылка на товар (FK → products.id) |
| `seller_id` | BIGINT | Ссылка на продавца (FK → sellers.id, nullable) |
| `operation_type` | VARCHAR(10) | Тип операции: "SELL" или "BUY" |
| `price` | DECIMAL(10,2) | Цена за единицу (может быть NULL для покупок) |
| `currency` | VARCHAR(10) | Валюта (например: "USD", "RUB", "u") |
| `quantity` | INTEGER | Количество товара |
| `condition` | VARCHAR(50) | Состояние (НОВЫЙ, Б/У, БУ) |
| `notes` | TEXT | Дополнительные условия ("от 20шт", "лотом") |
| `location` | VARCHAR(100) | Локация продажи (Москва, Регион) |
| `manufacturer` | VARCHAR(100) | Производитель (дублируется из Product) |
| `hashrate` | VARCHAR(50) | Мощность майнера (например: "104TH/s") |
| `seller_name` | VARCHAR(200) | Имя продавца (deprecated, используется seller.name) |
| `seller_phone` | VARCHAR(50) | Телефон продавца (deprecated, используется seller.phone) |
| `source_message_id` | VARCHAR(200) | ID сообщения WhatsApp |
| `source_chat_name` | VARCHAR(500) | Название чата |
| `additional_data` | TEXT | Дополнительные данные из Ollama (JSON) |
| `created_at` | TIMESTAMP | Время создания |
| `updated_at` | TIMESTAMP | Время последнего обновления |

**Связи:**
- `ManyToOne` → `Product` (много предложений принадлежат одному товару)
- `ManyToOne` → `Seller` (много предложений принадлежат одному продавцу)

#### 3. **Seller** (Продавец)
Таблица: `sellers`

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | BIGINT | Первичный ключ |
| `name` | VARCHAR(200) | Имя продавца из WhatsApp |
| `phone` | VARCHAR(50) | Телефон продавца (уникальный) |
| `whatsapp_id` | VARCHAR(200) | ID продавца из WhatsApp (senderId) |
| `contact_info` | TEXT | Дополнительная контактная информация |
| `rating` | DOUBLE | Рейтинг продавца (для будущего функционала) |
| `deals_count` | INTEGER | Количество сделок (для будущего функционала) |
| `created_at` | TIMESTAMP | Время создания |
| `updated_at` | TIMESTAMP | Время последнего обновления |

**Связи:**
- `OneToMany` → `Offer` (один продавец может иметь много предложений)

#### 4. **WhatsAppMessage** (Сообщение WhatsApp)
Таблица: `whatsapp_messages`

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | BIGINT | Первичный ключ |
| `message_id` | VARCHAR(200) | Уникальный ID сообщения из WhatsApp |
| `chat_id` | VARCHAR(200) | ID чата |
| `chat_name` | VARCHAR(500) | Название чата |
| `chat_type` | VARCHAR(50) | Тип чата: "group" или "personal" |
| `sender_id` | VARCHAR(200) | ID отправителя |
| `sender_name` | VARCHAR(200) | Имя отправителя |
| `sender_phone_number` | VARCHAR(50) | Телефон отправителя |
| `content` | TEXT | Текст сообщения |
| `timestamp` | TIMESTAMP | Время отправки сообщения |
| `has_media` | BOOLEAN | Наличие медиа |
| `media_mimetype` | VARCHAR(100) | Тип медиа |
| `media_filename` | VARCHAR(500) | Имя файла медиа |
| `media_data` | TEXT | Медиа в base64 |
| `message_type` | VARCHAR(50) | Тип сообщения |
| `is_forwarded` | BOOLEAN | Переслано ли сообщение |
| `is_update` | BOOLEAN | Является ли обновлением предыдущего сообщения |
| `original_message_id` | VARCHAR(200) | ID оригинального сообщения (если is_update = true) |
| `parsed_data` | TEXT | Обработанные данные от Ollama (JSON) |
| `created_at` | TIMESTAMP | Время создания записи |
| `updated_at` | TIMESTAMP | Время последнего обновления |

#### 5. **Request** (Заявка)
Таблица: `requests`

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | BIGINT | Первичный ключ |
| `offer_id` | BIGINT | Ссылка на предложение (FK → offers.id) |
| `whatsapp_message_id` | BIGINT | Ссылка на сообщение (FK → whatsapp_messages.id, nullable) |
| `client_name` | VARCHAR(200) | Имя клиента |
| `client_phone` | VARCHAR(50) | Телефон клиента |
| `message` | TEXT | Сообщение от клиента (опционально) |
| `status` | VARCHAR(20) | Статус: "NEW", "PROCESSED", "CLOSED", "CANCELLED" |
| `admin_comment` | TEXT | Комментарий администратора |
| `created_at` | TIMESTAMP | Время создания |
| `updated_at` | TIMESTAMP | Время последнего обновления |

**Связи:**
- `ManyToOne` → `Offer` (одна заявка связана с одним предложением)
- `ManyToOne` → `WhatsAppMessage` (одна заявка может быть связана с одним сообщением)

---

## Контроллеры и страницы

### 1. **HomeController**
Контроллер главной страницы.

**Эндпоинты:**

#### `GET /`
Главная страница магазина.

**Шаблон:** `index-new.html`

**Метод:** `home()`

**Параметры запроса:**
- `page` (int, default: 0) — номер страницы для пагинации товаров
- `size` (int, default: 100) — размер страницы
- `chatType` (String, optional) — фильтр по типу чата (не используется)

**Данные для шаблона:**
- `totalMessages` (long) — общее количество сообщений
- `groupMessages` (long) — количество сообщений из групп
- `personalMessages` (long) — количество личных сообщений
- `products` (List<Product>) — список товаров для отображения (первые 12)

**Логика работы:**
1. Получает статистику сообщений через `WhatsAppMessageService`
2. Загружает первые 12 товаров из `ProductRepository` с сортировкой по `updatedAt DESC`
3. Батч-загружает все `offers` для этих товаров через `OfferRepository.findByProductIdIn()`
4. Вычисляет минимальную цену для каждого товара (только для предложений типа SELL)
5. Устанавливает URL изображения для каждого товара через `ImageUrlResolver`
6. Добавляет данные в модель и возвращает шаблон

**Используемые сервисы:**
- `WhatsAppMessageService.getTotalMessages()`
- `WhatsAppMessageService.getMessagesCountByType()`
- `ProductRepository.findAll()`
- `OfferRepository.findByProductIdIn()`
- `ImageUrlResolver.resolveImageUrl()`

---

#### `GET /api/messages`
REST API для получения сообщений в JSON формате (для AJAX).

**Метод:** `getMessagesJson()`

**Параметры запроса:**
- `page` (int, default: 0)
- `size` (int, default: 100)
- `chatType` (String, optional) — фильтр по типу чата

**Ответ (JSON):**
```json
{
  "messages": [...],
  "currentPage": 0,
  "totalPages": 5,
  "totalElements": 278,
  "totalMessages": 278,
  "groupMessages": 278,
  "personalMessages": 0,
  "chatType": "group"
}
```

**Используемые сервисы:**
- `WhatsAppMessageService.getAllMessages()` или `getMessagesByChatType()`

---

### 2. **ProductsController**
Контроллер для работы с товарами.

**Эндпоинты:**

#### `GET /products`
Страница каталога товаров.

**Шаблон:** `products-new.html`

**Метод:** `products()`

**Параметры запроса:**
- `page` (int, default: 0)
- `size` (int, default: 12)

**Данные для шаблона:**
- `products` (Page<Product>) — страница товаров
- `currentPage` (int) — текущая страница
- `totalProducts` (long) — общее количество товаров
- `totalOffers` (long) — общее количество предложений
- `productOperationInfo` (Map<Long, ProductOperationInfo>) — информация о типе операции для каждого товара

**Логика работы:**
1. Получает товары с пагинацией из `ProductRepository.findAll()`
2. Батч-загружает `offers` для всех товаров на странице
3. Для каждого товара определяет:
   - Количество предложений на продажу (SELL) и покупку (BUY)
   - Минимальную цену (только для SELL)
   - Общее количество товара
   - Основной тип операции (SELL или BUY)
4. Сохраняет информацию в `ProductOperationInfo` для каждого товара
5. Устанавливает URL изображения для каждого товара

**Используемые сервисы:**
- `ProductRepository.findAll()`
- `OfferRepository.findByProductIdIn()`
- `ProductService.getTotalProducts()`
- `ProductService.getTotalOffers()`
- `ImageUrlResolver.resolveImageUrl()`

---

#### `GET /products/table`
Страница таблицы всех товаров.

**Шаблон:** `products-table.html`

**Метод:** `productsTable()`

**Параметры запроса:**
- `page` (int, default: 0)
- `size` (int, default: 50)
- `sortBy` (String, default: "updatedAt") — колонка для сортировки
- `sortDir` (String, default: "DESC") — направление сортировки: "ASC" или "DESC"
- `manufacturer` (String, optional) — фильтр по производителю

**Данные для шаблона:**
- `productsPage` (Page<Product>) — страница товаров
- `currentPage` (int) — текущая страница
- `sortBy` (String) — текущая колонка сортировки
- `sortDir` (String) — текущее направление сортировки
- `manufacturers` (List<String>) — список уникальных производителей для фильтра
- `currentManufacturer` (String) — текущий выбранный производитель

**Логика работы:**
1. Получает список уникальных производителей из `ProductRepository.findDistinctManufacturers()`
2. Если указан фильтр `manufacturer`, получает товары через `ProductRepository.findByManufacturer()`, иначе через `ProductRepository.findAll()`
3. Применяет сортировку из параметров запроса
4. Батч-загружает `offers` для всех товаров на странице
5. Вычисляет минимальную цену для каждого товара (только для SELL)
6. Устанавливает URL изображения для каждого товара

**Используемые сервисы:**
- `ProductRepository.findDistinctManufacturers()`
- `ProductRepository.findByManufacturer()` или `findAll()`
- `OfferRepository.findByProductIdIn()`
- `ImageUrlResolver.resolveImageUrl()`

---

#### `GET /products/{id}`
Детальная страница товара с таблицей предложений.

**Шаблон:** `product-details-new.html`

**Метод:** `productDetails()`

**Параметры запроса:**
- `id` (Long, path variable) — ID товара
- `dateFilter` (String, optional) — фильтр по дате: "today", "3days", "week", "month"
- `sortBy` (String, default: "updatedAt") — колонка для сортировки
- `sortDir` (String, default: "DESC") — направление сортировки
- `page` (int, default: 0) — номер страницы предложений
- `size` (int, default: 10) — размер страницы предложений

**Данные для шаблона:**
- `product` (Product) — товар
- `offers` (List<Offer>) — список предложений на текущей странице
- `offersPage` (Page<Offer>) — страница предложений
- `sellOffers` (List<Offer>) — предложения на продажу (только на текущей странице)
- `buyOffers` (List<Offer>) — предложения на покупку (только на текущей странице)
- `minPrice` (BigDecimal) — минимальная цена среди всех предложений на продажу
- `currency` (String) — валюта минимальной цены
- `dateFilter` (String) — текущий фильтр по дате
- `sortBy` (String) — текущая колонка сортировки
- `sortDir` (String) — текущее направление сортировки
- `currentPage` (int) — текущая страница
- `pageSize` (int) — размер страницы

**Логика работы:**
1. Получает товар по ID через `ProductService.getProductById()`
2. Если товар не найден, возвращает страницу ошибки
3. Устанавливает URL изображения для товара
4. Преобразует `dateFilter` в `LocalDateTime dateFrom` (например, "today" → начало текущего дня)
5. Создает `Pageable` с сортировкой из параметров запроса
6. Получает предложения с пагинацией и фильтрацией через `ProductService.getOffersByProductIdWithPagination()`
7. Разделяет предложения на продажи (SELL) и покупки (BUY) для статистики
8. Получает все предложения на продажу через `ProductService.getOffersByProductId()` для расчета минимальной цены
9. Вычисляет минимальную цену и валюту из всех предложений на продажу
10. Добавляет данные в модель и возвращает шаблон

**Используемые сервисы:**
- `ProductService.getProductById()`
- `ProductService.getOffersByProductIdWithPagination()`
- `ProductService.getOffersByProductId()`
- `ImageUrlResolver.resolveImageUrl()`

---

#### `GET /api/products/{id}/offers`
REST API для получения предложений товара в JSON формате (для AJAX обновления таблицы).

**Метод:** `getOffersJson()`

**Параметры запроса:**
- `id` (Long, path variable) — ID товара
- `dateFilter` (String, optional) — фильтр по дате: "today", "3days", "week", "month"
- `operationType` (String, optional) — тип операции: "SELL" или "BUY"
- `hasPrice` (Boolean, optional) — фильтр "Без пустых цен": true — только с ценой
- `sortBy` (String, default: "updatedAt") — колонка для сортировки
- `sortDir` (String, default: "DESC") — направление сортировки
- `page` (int, default: 0) — номер страницы
- `size` (int, default: 20) — размер страницы

**Ответ (JSON):**
```json
{
  "content": [
    {
      "id": 1,
      "operationType": "SELL",
      "price": 300.00,
      "currency": "USD",
      "quantity": 1,
      "condition": "Б/У",
      "location": "Москва",
      "seller": {
        "id": 1,
        "name": "Иван Петров",
        "phone": "5511999999999"
      },
      "updatedAt": "2024-01-15T10:30:00"
    }
  ],
  "totalElements": 45,
  "totalPages": 3,
  "currentPage": 0,
  "pageSize": 20,
  "first": true,
  "last": false,
  "numberOfElements": 20
}
```

**Логика работы:**
1. Преобразует `dateFilter` в `LocalDateTime dateFrom`
2. Преобразует `operationType` из строки в enum `OperationType`
3. Создает `Pageable` с сортировкой
4. Получает предложения с фильтрацией через `ProductService.getOffersByProductIdWithFilters()`
5. Преобразует сущности `Offer` в DTO через `OfferDTO.fromEntity()`
6. Формирует ответ с метаданными пагинации

**Используемые сервисы:**
- `ProductService.getOffersByProductIdWithFilters()`

---

### 3. **MessagesController**
Контроллер для просмотра сообщений WhatsApp.

**Базовый путь:** `/messages`

**Эндпоинты:**

#### `GET /messages`
Страница со списком всех сообщений.

**Шаблон:** `messages.html`

**Метод:** `messagesList()`

**Параметры запроса:**
- `page` (int, default: 0)
- `size` (int, default: 20)

**Данные для шаблона:**
- `messages` (Page<WhatsAppMessage>) — страница сообщений
- `currentPage` (int) — текущая страница
- `totalPages` (int) — общее количество страниц
- `totalElements` (long) — общее количество сообщений

**Используемые сервисы:**
- `WhatsAppMessageService.getAllMessages()`

---

#### `GET /messages/{id}`
Страница просмотра конкретного сообщения.

**Шаблон:** `message-view.html`

**Метод:** `viewMessage()`

**Параметры запроса:**
- `id` (Long, path variable) — ID сообщения

**Данные для шаблона:**
- `message` (WhatsAppMessage) — сообщение
- `parsedData` (Object) — распарсенные данные от Ollama (объект)
- `parsedDataJson` (String) — распарсенные данные в формате JSON (для отображения)
- `originalMessageJson` (String) — оригинальное сообщение в формате JSON (для отображения)

**Логика работы:**
1. Получает сообщение по ID через `WhatsAppMessageService.getMessageById()`
2. Если сообщение не найдено, возвращает страницу ошибки
3. Парсит `parsedData` из JSON строки через `WhatsAppMessageService.parseParsedData()`
4. Форматирует JSON для красивого отображения в шаблоне
5. Форматирует оригинальное сообщение в JSON для отображения

**Используемые сервисы:**
- `WhatsAppMessageService.getMessageById()`
- `WhatsAppMessageService.parseParsedData()`

---

#### `GET /messages/{id}/json`
REST API для получения сообщения в JSON формате.

**Метод:** `getMessageJson()`

**Ответ (JSON):**
```json
{
  "id": 1,
  "messageId": "msg_123456",
  "chatId": "120363123456789@g.us",
  "chatName": "Продажа ASIC майнеров",
  "chatType": "group",
  "senderId": "5511999999999@c.us",
  "senderName": "Иван Петров",
  "senderPhoneNumber": "5511999999999",
  "content": "...",
  "timestamp": "2024-01-15T10:30:00",
  "parsedData": {...},
  "parsedDataRaw": "{...}"
}
```

---

### 4. **RequestController**
Контроллер для работы с заявками клиентов.

**Базовый путь:** `/requests`

**Эндпоинты:**

#### `GET /requests`
Страница со списком всех заявок.

**Шаблон:** `requests.html`

**Метод:** `requestsList()`

**Параметры запроса:**
- `page` (int, default: 0)
- `size` (int, default: 20)
- `status` (String, optional) — фильтр по статусу: "NEW", "PROCESSED", "CLOSED", "CANCELLED"

**Данные для шаблона:**
- `requests` (Page<Request>) — страница заявок
- `statistics` (Map<String, Long>) — статистика по статусам
- `currentStatus` (String) — текущий выбранный статус
- `currentPage` (int) — текущая страница
- `currentSize` (int) — размер страницы

**Используемые сервисы:**
- `RequestService.getAllRequests()` или `getRequestsByStatus()`
- `RequestService.getRequestStatistics()`

---

#### `GET /requests/{id}`
Страница просмотра детальной информации о заявке.

**Шаблон:** `request-details.html`

**Метод:** `requestDetails()`

**Параметры запроса:**
- `id` (Long, path variable) — ID заявки

**Данные для шаблона:**
- `request` (Request) — заявка
- `requestDTO` (RequestDTO) — заявка в формате DTO

**Используемые сервисы:**
- `RequestService.getRequestById()`

---

#### `POST /requests/api/create`
REST API для создания новой заявки.

**Метод:** `createRequest()`

**Тело запроса (JSON):**
```json
{
  "offerId": 1,
  "clientName": "Петр Иванов",
  "clientPhone": "+79991234567",
  "message": "Интересует этот товар"
}
```

**Ответ (JSON):**
```json
{
  "success": true,
  "message": "Заявка успешно создана",
  "requestId": 1
}
```

**Логика работы:**
1. Валидирует входящие данные (offerId, clientName, clientPhone обязательны)
2. Создает заявку через `RequestService.createRequest()`
3. Возвращает ответ с ID созданной заявки

**Используемые сервисы:**
- `RequestService.createRequest()`

---

#### `GET /requests/api/{id}`
REST API для получения заявки в JSON формате.

**Метод:** `getRequestJson()`

**Ответ (JSON):**
```json
{
  "id": 1,
  "offerId": 1,
  "clientName": "Петр Иванов",
  "clientPhone": "+79991234567",
  "message": "...",
  "status": "NEW",
  "createdAt": "2024-01-15T10:30:00"
}
```

---

### 5. **WebhookController**
REST API контроллер для приема сообщений от WhatsApp сервиса.

**Базовый путь:** `/api/webhook`

**Эндпоинты:**

#### `POST /api/webhook/whatsapp`
Прием сообщений от WhatsApp сервиса.

**Метод:** `receiveWhatsAppMessage()`

**Заголовки:**
- `Content-Type: application/json;charset=UTF-8`
- `X-API-Key` (String, optional) — API ключ для авторизации (не используется)

**Тело запроса (JSON):**
```json
{
  "messageId": "msg_123456",
  "chatId": "120363123456789@g.us",
  "chatName": "Продажа ASIC майнеров",
  "chatType": "group",
  "senderId": "5511999999999@c.us",
  "senderName": "Иван Петров",
  "senderPhoneNumber": "5511999999999",
  "content": "Продам Москва Б/У:\n1)S19j PRO 104T, 1шт. 270u;...",
  "timestamp": "2024-01-15T10:30:00Z",
  "hasMedia": false,
  "messageType": "chat",
  "isForwarded": false,
  "parsedData": {
    "operationType": "SELL",
    "location": "Москва",
    "products": [
      {
        "model": "S19j PRO 104T",
        "quantity": 1,
        "price": 270,
        "currency": "u",
        "condition": "Б/У",
        "hashrate": "104TH/s",
        "manufacturer": "Bitmain"
      }
    ]
  }
}
```

**Ответ (JSON):**
```json
{
  "success": true,
  "message": "Сообщение успешно сохранено и обновлены существующие предложения",
  "messageId": 1
}
```

**Логика работы:**
1. Логирует входящие данные для диагностики кодировки
2. Проверяет, есть ли предыдущие сообщения от этого продавца через `WhatsAppMessageService.findPreviousMessageIdFromSeller()` для определения обновлений
3. Сохраняет сообщение через `WhatsAppMessageService.saveMessage()`
4. Если в сообщении есть `parsedData` (обработанные данные от Ollama), обрабатывает их через `ProductService.processParsedData()`
5. Если обнаружено обновление существующих предложений, устанавливает флаг `isUpdate` и `originalMessageId` в сообщении
6. Возвращает ответ с результатом операции

**Используемые сервисы:**
- `WhatsAppMessageService.findPreviousMessageIdFromSeller()`
- `WhatsAppMessageService.saveMessage()`
- `WhatsAppMessageService.updateMessage()`
- `ProductService.processParsedData()`

---

#### `GET /api/webhook/health`
Health check endpoint.

**Метод:** `health()`

**Ответ (JSON):**
```json
{
  "success": true,
  "message": "API работает. Сообщений в БД: 278",
  "messageId": null
}
```

**Используемые сервисы:**
- `WhatsAppMessageService.getTotalMessages()`

---

#### `GET /api/webhook/messages/count`
Endpoint для проверки количества сообщений (для диагностики).

**Метод:** `getMessagesCount()`

**Ответ (JSON):**
```json
{
  "total": 278,
  "groups": 278,
  "personal": 0
}
```

---

## API Endpoints

### Сводная таблица всех API endpoints:

| Метод | Путь | Описание | Контроллер |
|-------|------|----------|------------|
| `GET` | `/` | Главная страница | `HomeController` |
| `GET` | `/api/messages` | Получение сообщений в JSON | `HomeController` |
| `GET` | `/products` | Каталог товаров | `ProductsController` |
| `GET` | `/products/table` | Таблица товаров | `ProductsController` |
| `GET` | `/products/{id}` | Детальная страница товара | `ProductsController` |
| `GET` | `/api/products/{id}/offers` | Получение предложений в JSON | `ProductsController` |
| `GET` | `/messages` | Список сообщений | `MessagesController` |
| `GET` | `/messages/{id}` | Просмотр сообщения | `MessagesController` |
| `GET` | `/messages/{id}/json` | Получение сообщения в JSON | `MessagesController` |
| `GET` | `/requests` | Список заявок | `RequestController` |
| `GET` | `/requests/{id}` | Просмотр заявки | `RequestController` |
| `POST` | `/requests/api/create` | Создание заявки | `RequestController` |
| `GET` | `/requests/api/{id}` | Получение заявки в JSON | `RequestController` |
| `POST` | `/api/webhook/whatsapp` | Прием сообщений от WhatsApp | `WebhookController` |
| `GET` | `/api/webhook/health` | Health check | `WebhookController` |
| `GET` | `/api/webhook/messages/count` | Количество сообщений | `WebhookController` |

---

## Сервисы и бизнес-логика

### 1. **ProductService**
Сервис для работы с товарами и предложениями.

**Основные методы:**

#### `processParsedData(Map<String, Object> parsedData, String messageId, String chatName, String sellerName, String sellerPhone, String location): boolean`
Обрабатывает распарсенные данные от Ollama и сохраняет товары с предложениями.

**Параметры:**
- `parsedData` — распарсенные данные от Ollama (Map с полями: operationType, location, products[])
- `messageId` — ID сообщения из WhatsApp
- `chatName` — название чата
- `sellerName` — имя продавца
- `sellerPhone` — телефон продавца
- `location` — локация продажи

**Логика работы:**
1. Извлекает тип операции (SELL или BUY) из `parsedData.operationType`
2. Извлекает список товаров из `parsedData.products[]`
3. Находит или создает продавца через `SellerService.findOrCreateSeller()`
4. Проверяет, есть ли уже предложения от этого продавца (для определения обновлений)
5. Для каждого товара из списка:
   - Вызывает `processProduct()` для обработки отдельного товара
   - Если товар был обновлен, увеличивает счетчик обновлений
   - Если создан новый, увеличивает счетчик созданий
6. Возвращает `true`, если были обновления, `false` если только новые предложения

**Используемые репозитории:**
- `ProductRepository.findByModel()`
- `ProductRepository.save()`
- `OfferRepository.findByProductIdAndSellerId()`
- `OfferRepository.save()`

**Используемые сервисы:**
- `SellerService.findOrCreateSeller()`

---

#### `processProduct(Map<String, Object> productData, String messageId, String chatName, Seller seller, String location, OperationType operationType, boolean checkForDuplicates): boolean`
Обрабатывает один товар и создает/обновляет предложение.

**Параметры:**
- `productData` — данные товара (Map с полями: model, price, quantity, condition, notes, hashrate, manufacturer, и т.д.)
- `messageId` — ID сообщения
- `chatName` — название чата
- `seller` — продавец (сущность)
- `location` — локация
- `operationType` — тип операции (SELL или BUY)
- `checkForDuplicates` — проверять ли на дубликаты

**Логика работы:**
1. Извлекает модель товара из `productData.model`
2. Находит или создает товар через `ProductRepository.findByModel()`
3. Если товар новый, сохраняет производителя из `productData.manufacturer`
4. Если у существующего товара нет производителя, обновляет его из `productData.manufacturer`
5. Если `checkForDuplicates = true`, ищет существующее предложение от этого продавца для этой модели с таким же типом операции
6. Если предложение найдено, обновляет его, иначе создает новое
7. Заполняет/обновляет поля предложения:
   - `operationType` (SELL или BUY)
   - `price` (может быть null для покупок)
   - `currency` (преобразует "u" в "USD")
   - `quantity`
   - `condition`
   - `notes`
   - `location`
   - `hashrate`
   - `manufacturer`
   - `seller` (связь с сущностью Seller)
   - `sellerName`, `sellerPhone` (для обратной совместимости)
   - `sourceMessageId`, `sourceChatName`
   - `additionalData` (остальные поля из `productData` в формате JSON)
8. Сохраняет предложение через `OfferRepository.save()`
9. Обновляет `updatedAt` товара, чтобы он всплывал в списке
10. Возвращает `true`, если предложение было обновлено, `false` если создано новое

**Используемые репозитории:**
- `ProductRepository.findByModel()`
- `ProductRepository.save()`
- `OfferRepository.findByProductIdAndSellerId()`
- `OfferRepository.save()`

---

#### `getOffersByProductIdWithFilters(Long productId, LocalDateTime dateFrom, OperationType operationType, Boolean hasPrice, Pageable pageable): Page<Offer>`
Получает предложения для товара с пагинацией и фильтрацией.

**Параметры:**
- `productId` — ID товара
- `dateFrom` — дата начала периода (может быть null)
- `operationType` — тип операции (SELL или BUY, может быть null)
- `hasPrice` — только с ценой (true) или все (false/null)
- `pageable` — пагинация и сортировка

**Логика работы:**
1. Вычисляет LIMIT и OFFSET из `Pageable`
2. Преобразует `OperationType` в строку для SQL запроса
3. Извлекает параметры сортировки из `Pageable`:
   - Преобразует camelCase в snake_case через `convertCamelCaseToSnakeCase()`
   - Валидирует колонку через `isValidSortColumn()` (защита от SQL инъекций)
4. Строит динамический SQL запрос:
   - Фильтр по `product_id`
   - Фильтр по `updated_at >= COALESCE(:dateFrom, '1900-01-01'::timestamp)`
   - Фильтр по `operation_type` (если указан)
   - Фильтр по `price IS NOT NULL` (если `hasPrice = true`)
   - Сортировка по указанной колонке
   - LIMIT и OFFSET для пагинации
5. Выполняет запрос через `EntityManager.createNativeQuery()`
6. Получает общее количество через `OfferRepository.countByProductIdWithFilters()` (с учетом фильтра по цене, если активен)
7. Инициализирует продавцов для каждого предложения (для избежания LazyInitializationException)
8. Создает объект `Page` вручную через `PageImpl`

**Используемые репозитории:**
- `OfferRepository.countByProductIdWithFilters()`

**Используемые утилиты:**
- `EntityManager` (JPA)

---

#### `getProductById(Long id): Optional<Product>`
Получает товар по ID.

**Примечание:** Предложения нужно получать отдельно через `getOffersByProductId()` чтобы избежать LazyInitializationException.

---

#### `getOffersByProductId(Long productId): List<Offer>`
Получает все предложения для товара с загрузкой продавцов.

**Логика:**
1. Получает все предложения через `OfferRepository.findByProductIdOrderByPriceAsc()` (использует JOIN FETCH)
2. Разделяет на продажи (SELL) и покупки (BUY)
3. Сортирует продажи по цене (от меньшей к большей)
4. Сортирует покупки по дате обновления (новые сначала)
5. Объединяет: сначала продажи, потом покупки

---

### 2. **WhatsAppMessageService**
Сервис для работы с сообщениями WhatsApp.

**Основные методы:**

#### `saveMessage(WhatsAppMessageDTO messageDTO, String originalMessageId): WhatsAppMessage`
Сохраняет сообщение в базу данных.

**Логика:**
1. Преобразует DTO в сущность `WhatsAppMessage`
2. Если указан `originalMessageId`, устанавливает флаг `isUpdate = true`
3. Сохраняет через `WhatsAppMessageRepository.save()`
4. Возвращает сохраненную сущность

---

#### `findPreviousMessageIdFromSeller(String sellerPhone, String chatId, String currentMessageId): String`
Находит ID предыдущего сообщения от того же продавца в том же чате.

**Используется для определения обновлений.**

---

#### `parseParsedData(String parsedDataJson): Object`
Парсит JSON строку `parsedData` в объект для удобной работы.

---

### 3. **SellerService**
Сервис для работы с продавцами.

**Основные методы:**

#### `findOrCreateSeller(String phone, String name, String whatsappId): Seller`
Находит или создает продавца по телефону.

**Логика:**
1. Ищет продавца по телефону через `SellerRepository.findByPhone()`
2. Если найден, обновляет имя (если передано новое)
3. Если не найден, создает нового продавца
4. Сохраняет через `SellerRepository.save()`
5. Возвращает продавца

---

### 4. **RequestService**
Сервис для работы с заявками.

**Основные методы:**

#### `createRequest(RequestDTO.CreateRequestDTO createDTO): Request`
Создает новую заявку.

**Логика:**
1. Получает предложение по ID через `OfferRepository.findById()`
2. Создает новую заявку со статусом `NEW`
3. Сохраняет через `RequestRepository.save()`
4. Возвращает созданную заявку

---

## Связи между компонентами

### Схема взаимодействия компонентов:

```
WhatsApp Service (Node.js)
    ↓ HTTP POST
WebhookController
    ↓
WhatsAppMessageService (сохраняет сообщение)
    ↓
ProductService.processParsedData()
    ↓
SellerService.findOrCreateSeller()
    ↓
ProductService.processProduct()
    ↓
ProductRepository / OfferRepository (сохраняет данные)
    ↓
База данных (PostgreSQL)
```

### Схема отображения страниц:

```
Пользователь (браузер)
    ↓ HTTP GET
Controller (MVC)
    ↓
Service (бизнес-логика)
    ↓
Repository (доступ к БД)
    ↓
База данных (PostgreSQL)
    ↓
Service (обработка данных)
    ↓
Controller (добавление в модель)
    ↓
Thymeleaf (рендеринг шаблона)
    ↓
HTML ответ
```

### Схема AJAX обновлений:

```
Пользователь (JavaScript)
    ↓ AJAX GET
Controller (REST API)
    ↓
Service (бизнес-логика)
    ↓
Repository (доступ к БД)
    ↓
База данных (PostgreSQL)
    ↓
Service (обработка данных)
    ↓
Controller (DTO преобразование)
    ↓
JSON ответ
    ↓
JavaScript (обновление DOM)
```

---

## Потоки данных

### 1. Поток приема сообщения от WhatsApp:

1. **WhatsApp Service** получает сообщение из WhatsApp
2. **WhatsApp Service** отправляет HTTP POST на `/api/webhook/whatsapp`
3. **WebhookController** принимает запрос с `WhatsAppMessageDTO`
4. **WhatsAppMessageService** сохраняет сообщение в таблицу `whatsapp_messages`
5. Если в сообщении есть `parsedData`:
   - **ProductService.processParsedData()** извлекает тип операции и список товаров
   - **SellerService.findOrCreateSeller()** находит или создает продавца
   - Для каждого товара:
     - **ProductService.processProduct()** находит или создает товар
     - Проверяет на дубликаты (существующее предложение от этого продавца)
     - Создает или обновляет предложение в таблице `offers`
     - Обновляет `updated_at` товара в таблице `products`
6. Возвращается ответ об успешном сохранении

### 2. Поток отображения главной страницы:

1. Пользователь открывает `/`
2. **HomeController.home()** вызывается
3. **WhatsAppMessageService** получает статистику сообщений
4. **ProductRepository** загружает первые 12 товаров с сортировкой по `updated_at DESC`
5. **OfferRepository.findByProductIdIn()** батч-загружает все предложения для этих товаров
6. Для каждого товара вычисляется минимальная цена (только для SELL)
7. **ImageUrlResolver** устанавливает URL изображения для каждого товара
8. Данные добавляются в модель Thymeleaf
9. Шаблон `index-new.html` рендерится с данными
10. HTML ответ отправляется пользователю

### 3. Поток отображения детальной страницы товара:

1. Пользователь открывает `/products/{id}`
2. **ProductsController.productDetails()** вызывается
3. **ProductService.getProductById()** получает товар
4. Параметры фильтров преобразуются (dateFilter → LocalDateTime)
5. **ProductService.getOffersByProductIdWithPagination()** получает предложения с фильтрацией и пагинацией:
   - Строится динамический SQL запрос
   - Применяются фильтры: дата, тип операции, наличие цены
   - Применяется сортировка
   - Применяется пагинация (LIMIT/OFFSET)
6. Предложения разделяются на продажи (SELL) и покупки (BUY)
7. **ProductService.getOffersByProductId()** получает все предложения для расчета минимальной цены
8. Вычисляется минимальная цена и валюта
9. Данные добавляются в модель Thymeleaf
10. Шаблон `product-details-new.html` рендерится
11. HTML ответ отправляется пользователю

### 4. Поток AJAX обновления таблицы предложений:

1. Пользователь применяет фильтры (дата, тип операции, наличие цены) или меняет сортировку/пагинацию
2. JavaScript отправляет AJAX GET на `/api/products/{id}/offers` с параметрами фильтров
3. **ProductsController.getOffersJson()** вызывается
4. Параметры фильтров преобразуются (dateFilter → LocalDateTime, operationType → enum)
5. **ProductService.getOffersByProductIdWithFilters()** получает предложения:
   - Строится динамический SQL запрос с фильтрами
   - Выполняется запрос через EntityManager
   - Получается общее количество для пагинации
6. Сущности `Offer` преобразуются в `OfferDTO` через `OfferDTO.fromEntity()`
7. Формируется JSON ответ с метаданными пагинации
8. JavaScript получает ответ и обновляет таблицу в DOM без перезагрузки страницы

### 5. Поток создания заявки:

1. Пользователь заполняет форму заявки на странице товара
2. JavaScript отправляет POST на `/requests/api/create` с данными (offerId, clientName, clientPhone, message)
3. **RequestController.createRequest()** вызывается
4. Валидируются входящие данные (offerId, clientName, clientPhone обязательны)
5. **RequestService.createRequest()** вызывается:
   - Получает предложение по ID через `OfferRepository.findById()`
   - Создает новую заявку со статусом `NEW`
   - Сохраняет через `RequestRepository.save()`
6. Возвращается JSON ответ с ID созданной заявки
7. JavaScript показывает сообщение об успешном создании заявки

---

## Дополнительные сведения

### Батч-загрузка (Batch Loading)
Для избежания проблемы N+1 запросов используется батч-загрузка предложений:
- Вместо загрузки предложений для каждого товара отдельно
- Загружаются все предложения для всех товаров одним запросом через `OfferRepository.findByProductIdIn()`
- Затем предложения группируются по `product_id` и назначаются каждому товару

### Ленивая загрузка (Lazy Loading)
Для избежания `LazyInitializationException`:
- В контроллерах используется `@Transactional(readOnly = true)` для поддержания сессии Hibernate
- Перед возвратом данных из сервисов принудительно инициализируются связанные сущности (например, `seller.getName()`)
- В AJAX эндпоинтах сущности преобразуются в DTO перед возвратом

### Динамический SQL для фильтрации
Для поддержки фильтров по дате, типу операции и наличию цены используется динамическое построение SQL запроса через `EntityManager`:
- Параметры сортировки преобразуются из camelCase в snake_case
- Валидация колонок предотвращает SQL инъекции
- Используется `COALESCE` и `CAST` для корректной работы с NULL значениями в PostgreSQL

### Обновление vs Создание предложений
Система автоматически определяет, является ли новое предложение обновлением существующего:
- Проверяется наличие предложения от того же продавца для той же модели с тем же типом операции
- Если найдено, обновляется существующее предложение
- Если не найдено, создается новое предложение
- Это позволяет избежать дубликатов при повторных сообщениях от продавца

---

*Документация создана: 2024-01-15*
*Последнее обновление: 2024-01-15*

