# Инструкция по деплою приложения

## Оптимизации для production

Приложение настроено с кэшированием и оптимизациями для production. Для запуска в production режиме используйте профиль `prod`.

## Запуск в production режиме

### 1. Установка переменных окружения

Создайте файл `.env` или установите переменные окружения:

```bash
# База данных PostgreSQL
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/miners
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_password

# Профиль Spring Boot
SPRING_PROFILES_ACTIVE=prod

# Директория для загружаемых файлов
APP_UPLOAD_DIR=/var/www/miners/images

# Кэширование Thymeleaf (включено в prod)
THYMELEAF_CACHE=true

# Сжатие ответов сервера
SERVER_COMPRESSION_ENABLED=true
```

### 2. Сборка приложения

```bash
cd shop-backend
mvn clean package -DskipTests
```

JAR файл будет создан в `target/shop-backend-1.0.0.jar`

### 3. Запуск приложения

```bash
# С профилем production
java -jar -Dspring.profiles.active=prod target/shop-backend-1.0.0.jar

# Или с переменными окружения
SPRING_PROFILES_ACTIVE=prod java -jar target/shop-backend-1.0.0.jar
```

## Включенные оптимизации

### 1. Кэширование

- **Spring Cache (Caffeine)**: Кэширование данных с TTL 5 минут
  - Данные криптовалют (`CryptoDataService`)
  - Списки производителей, серий, алгоритмов (`MinerDetailService`)
  
- **Thymeleaf Cache**: Кэширование шаблонов (включено в production)
  
- **Статические ресурсы**: Кэширование на 1 год для статических файлов (CSS, JS, изображения)

### 2. База данных

- **HikariCP Connection Pool**: Оптимизированный пул соединений
  - Минимум: 5 соединений
  - Максимум: 20 соединений
  - Таймауты настроены для production
  
- **JPA/Hibernate**: Оптимизация для production
  - Batch processing включен
  - SQL логирование отключено
  - Форматирование SQL отключено

### 3. HTTP сервер

- **Gzip сжатие**: Включено для всех текстовых ресурсов
  - HTML, CSS, JavaScript, JSON, XML
  - Минимальный размер для сжатия: 1KB
  
- **HTTP/2**: Включено (если поддерживается)

### 4. Логирование

- **Production уровень**: INFO для приложения, WARN для фреймворков
- **Размер логов**: Максимум 50MB на файл
- **История логов**: 30 файлов

## Мониторинг производительности

### Статистика кэша

Spring Cache с Caffeine автоматически собирает статистику. Для просмотра статистики можно добавить эндпоинт:

```java
@RestController
public class CacheStatsController {
    @Autowired
    private CacheManager cacheManager;
    
    @GetMapping("/api/cache/stats")
    public Map<String, Object> getCacheStats() {
        // Реализация получения статистики
    }
}
```

### Мониторинг базы данных

HikariCP предоставляет метрики через JMX. Для включения:

```yaml
spring:
  datasource:
    hikari:
      register-mbeans: true
```

## Рекомендации по деплою

### 1. Использование reverse proxy (Nginx)

Рекомендуется использовать Nginx перед приложением:

```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    # Сжатие
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
    
    # Кэширование статических ресурсов
    location ~* \.(jpg|jpeg|png|gif|ico|css|js|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
    
    # Проксирование на Spring Boot
    location / {
        proxy_pass http://localhost:8050;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 2. Systemd service (Linux)

Создайте файл `/etc/systemd/system/miners-shop.service`:

```ini
[Unit]
Description=Miners Shop Backend
After=network.target postgresql.service

[Service]
Type=simple
User=miners
WorkingDirectory=/opt/miners/shop-backend
ExecStart=/usr/bin/java -jar -Dspring.profiles.active=prod /opt/miners/shop-backend/target/shop-backend-1.0.0.jar
Restart=always
RestartSec=10
Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="APP_UPLOAD_DIR=/var/www/miners/images"

[Install]
WantedBy=multi-user.target
```

Запуск:
```bash
sudo systemctl enable miners-shop
sudo systemctl start miners-shop
sudo systemctl status miners-shop
```

### 3. Мониторинг

Рекомендуется настроить мониторинг:
- **Prometheus + Grafana**: Для метрик приложения
- **ELK Stack**: Для централизованного логирования
- **Health checks**: Используйте `/actuator/health` (если включен Spring Boot Actuator)

## Проверка производительности

После деплоя проверьте:

1. **Время отклика**: Должно быть < 200ms для кэшированных запросов
2. **Использование памяти**: Мониторинг через JVM метрики
3. **Размер кэша**: Проверьте статистику Caffeine
4. **Соединения БД**: Мониторинг HikariCP пула

## Отключение оптимизаций для разработки

Для разработки используйте профиль по умолчанию (без `prod`):

```bash
mvn spring-boot:run
```

Или явно укажите профиль `dev`:

```bash
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

В режиме разработки:
- Кэширование Thymeleaf отключено
- SQL логирование включено (если нужно)
- Более детальное логирование

