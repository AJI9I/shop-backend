# Shop Backend - Интернет-магазин майнеров

Spring Boot приложение для приема и отображения сообщений из WhatsApp.

## Технологии

- **Java 23**
- **Spring Boot 3.2.0**
- **Spring Data JPA**
- **Thymeleaf** (шаблоны)
- **Bootstrap 5** (UI)
- **H2 Database** (для разработки, можно переключить на PostgreSQL)

## Структура проекта

```
shop-backend/
├── src/main/java/com/miners/shop/
│   ├── ShopApplication.java          # Главный класс приложения
│   ├── controller/
│   │   ├── HomeController.java       # Контроллер главной страницы
│   │   └── WebhookController.java    # REST API для приема сообщений
│   ├── entity/
│   │   └── WhatsAppMessage.java      # JPA сущность сообщения
│   ├── repository/
│   │   └── WhatsAppMessageRepository.java  # JPA репозиторий
│   ├── service/
│   │   └── WhatsAppMessageService.java     # Сервис для работы с сообщениями
│   └── dto/
│       └── WhatsAppMessageDTO.java         # DTO для входящих сообщений
├── src/main/resources/
│   ├── application.yml               # Конфигурация
│   ├── templates/
│   │   └── index.html                # Главная страница
│   └── static/                       # Статические ресурсы
└── pom.xml                           # Maven конфигурация
```

## Установка и запуск

### Требования

- Java 23
- Maven 3.6+

### Запуск

1. Соберите проект:
```bash
mvn clean install
```

2. Запустите приложение:
```bash
mvn spring-boot:run
```

3. Откройте в браузере:
```
http://localhost:8050
```

## API Endpoints

### Прием сообщений от WhatsApp сервиса

**POST** `/api/webhook/whatsapp`

Принимает JSON с данными сообщения:

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
  "isForwarded": false
}
```

**Ответ:**
```json
{
  "success": true,
  "message": "Сообщение успешно сохранено",
  "messageId": 1
}
```

### Health Check

**GET** `/api/webhook/health`

Проверка работоспособности API.

## Главная страница

Главная страница (`/`) отображает:
- Статистику сообщений
- Таблицу всех сообщений с пагинацией
- Фильтры по типу чата (группы/личные)
- Автоматическое обновление каждые 30 секунд

## База данных

По умолчанию используется H2 in-memory база данных. Данные сохраняются в таблицу `whatsapp_messages`.

### Переключение на PostgreSQL

В `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/shopdb
    username: postgres
    password: your_password
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
```

## Конфигурация

Основные настройки в `application.yml`:
- Порт сервера: `server.port` (по умолчанию: 8080)
- База данных: настройки в `spring.datasource`
- Логирование: `logging.level`

## Интеграция с WhatsApp сервисом

Настройте WhatsApp сервис на отправку сообщений по адресу:
```
http://localhost:8050/api/webhook/whatsapp
```

В веб-интерфейсе WhatsApp сервиса укажите:
- **API URL**: `http://localhost:8050`
- **API Endpoint**: `/api/webhook/whatsapp`
