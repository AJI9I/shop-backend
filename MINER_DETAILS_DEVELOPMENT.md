# Документация: Разработка таблицы MinerDetails

## Оглавление

1. [Описание задачи](#описание-задачи)
2. [Архитектурное решение](#архитектурное-решение)
3. [Структура таблицы](#структура-таблицы)
4. [Пошаговая реализация](#пошаговая-реализация)
5. [Примеры кода](#примеры-кода)
6. [Интеграция с существующим кодом](#интеграция-с-существующим-кодом)
7. [Тестирование](#тестирование)

---

## Описание задачи

### Проблема
В настоящее время данные о товарах (майнерах) хранятся в таблице `products` напрямую из данных, полученных от нейросети через WhatsApp. Это не позволяет:
- Редактировать название товара для стандартизации
- Изменять производителя или серию независимо от исходных данных
- Добавлять детальное описание администратором или ИИ
- Объединять несколько товаров из `products` в один стандартизированный продукт для отображения на сайте

### Решение
Создать таблицу `miner_details` для хранения редактируемых данных о майнерах, которые будут использоваться для отображения на сайте. Таблица `products` остается неизменной и используется для связи с предложениями (`offers`).

### Связи
- **Product** (Many) → **MinerDetail** (One)
- Несколько товаров из `products` могут ссылаться на одну запись в `miner_details` (объединение)
- При создании нового `Product` автоматически создается `MinerDetail` с данными из нейросети
- Администратор может редактировать `MinerDetail` независимо от исходных данных в `products`

---

## Архитектурное решение

### Схема связей

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│   Product   │────────▶│ MinerDetail  │         │    Offer    │
│             │         │              │         │             │
│ id          │         │ id           │         │ id          │
│ model       │         │ standard_name│         │ product_id  │
│ manufacturer│         │ manufacturer │         │ price       │
│ ...         │         │ series       │         │ ...         │
└─────────────┘         │ hashrate     │         └─────────────┘
      │                 │ algorithm    │                │
      │                 │ ...          │                │
      └─────────────────┴──────────────┘                │
                        ▲                               │
                        │                               │
                        └───────────────────────────────┘
                    (через Product.productDetailId)
```

### Таблица связей `product_miner_detail` (опционально)

Для поддержки объединения нескольких `products` в один `MinerDetail`:

```
┌─────────────┐         ┌──────────────────────┐         ┌──────────────┐
│   Product   │────────▶│ product_miner_detail │────────▶│ MinerDetail  │
│             │         │                      │         │              │
│ id          │         │ product_id (FK)      │         │ id           │
│ ...         │         │ miner_detail_id (FK) │         │ ...          │
└─────────────┘         └──────────────────────┘         └──────────────┘
```

**Вариант 1 (простой):** Прямая связь через поле `product_detail_id` в таблице `products`
- Один `Product` → один `MinerDetail` (через поле `product_detail_id`)
- Для объединения устанавливаем одинаковое значение `product_detail_id` у всех объединяемых `Product` (например, все товары получают `product_detail_id = 1`)

**Вариант 2 (гибкий):** Промежуточная таблица `product_miner_detail`
- Несколько `Product` → один `MinerDetail`
- Более гибкое объединение товаров

**Рекомендация:** Начать с **Варианта 1** для простоты, при необходимости мигрировать на **Вариант 2**.

---

## Структура таблицы

### Таблица: `miner_details`

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Первичный ключ |
| `standard_name` | VARCHAR(200) | NOT NULL | Стандартизированное название (например: "Antminer S21E XP Hydro 3U") |
| `manufacturer` | VARCHAR(100) | | Производитель (Bitmain, MicroBT, Canaan, и т.д.) |
| `series` | VARCHAR(100) | | Серия (например: "S21", "L7", "S19j") |
| `hashrate` | VARCHAR(50) | | Хэшрейт (например: "860 TH/s", "104 TH/s") |
| `algorithm` | VARCHAR(50) | | Алгоритм майнинга (SHA-256, Scrypt, и т.д.) |
| `power_consumption` | VARCHAR(50) | | Потребление энергии (например: "11180 Вт/ч", "3250W") |
| `coins` | TEXT | | Добываемые монеты (JSON массив: ["BTC", "BCH", "BCV"] или просто текст: "BTC/BCH/BCV") |
| `power_source` | VARCHAR(100) | | Источник питания ("Интегрированный", "Внешний") |
| `cooling` | VARCHAR(100) | | Тип охлаждения ("Водяное", "Воздушное", "Гибридное") |
| `operating_temperature` | VARCHAR(100) | | Рабочая температура (например: "от 0 до 40 °С", "-5°C to 45°C") |
| `dimensions` | VARCHAR(200) | | Размеры (например: "900 x 486.2 x 132 мм" или JSON: {"length": 900, "width": 486.2, "height": 132, "unit": "мм"}) |
| `noise_level` | VARCHAR(50) | | Уровень шума (например: "40 дБ", "75dB") |
| `description` | TEXT | | Основное описание майнера (генерируется ИИ или заполняется администратором) |
| `features` | TEXT | | Особенности оборудования (расширенное описание) |
| `placement_info` | TEXT | | Рекомендации по размещению (где лучше разместить майнер) |
| `producer_info` | TEXT | | Информация о производителе |
| `created_at` | TIMESTAMP | NOT NULL | Время создания записи |
| `updated_at` | TIMESTAMP | NOT NULL | Время последнего обновления |

### Индексы

- `INDEX idx_manufacturer (manufacturer)` - для фильтрации по производителю
- `INDEX idx_series (series)` - для фильтрации по серии
- `INDEX idx_algorithm (algorithm)` - для фильтрации по алгоритму

---

## Пошаговая реализация

### Шаг 1: Создание Entity `MinerDetail`

**Файл:** `shop-backend/src/main/java/com/miners/shop/entity/MinerDetail.java`

```java
package com.miners.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Сущность детальной информации о майнере
 * Расширение таблицы products с редактируемыми данными для отображения на сайте
 */
@Entity
@Table(name = "miner_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MinerDetail {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Стандартизированное название майнера
     * (редактируемое администратором, не зависит от product.model)
     */
    @Column(nullable = false, length = 200)
    private String standardName;
    
    /**
     * Производитель майнера (редактируемый)
     */
    @Column(length = 100)
    private String manufacturer;
    
    /**
     * Серия майнера (например: "S21", "L7", "S19j")
     */
    @Column(length = 100)
    private String series;
    
    /**
     * Хэшрейт (например: "860 TH/s", "104 TH/s")
     */
    @Column(length = 50)
    private String hashrate;
    
    /**
     * Алгоритм майнинга (SHA-256, Scrypt, и т.д.)
     */
    @Column(length = 50)
    private String algorithm;
    
    /**
     * Потребление энергии (например: "11180 Вт/ч", "3250W")
     */
    @Column(length = 50)
    private String powerConsumption;
    
    /**
     * Добываемые монеты (JSON массив или текст)
     * Формат: ["BTC", "BCH", "BCV"] или "BTC/BCH/BCV"
     */
    @Column(columnDefinition = "TEXT")
    private String coins;
    
    /**
     * Источник питания ("Интегрированный", "Внешний")
     */
    @Column(length = 100)
    private String powerSource;
    
    /**
     * Тип охлаждения ("Водяное", "Воздушное", "Гибридное")
     */
    @Column(length = 100)
    private String cooling;
    
    /**
     * Рабочая температура (например: "от 0 до 40 °С")
     */
    @Column(length = 100)
    private String operatingTemperature;
    
    /**
     * Размеры майнера (например: "900 x 486.2 x 132 мм")
     */
    @Column(length = 200)
    private String dimensions;
    
    /**
     * Уровень шума (например: "40 дБ", "75dB")
     */
    @Column(length = 50)
    private String noiseLevel;
    
    /**
     * Основное описание майнера
     * (генерируется ИИ или заполняется администратором)
     */
    @Column(columnDefinition = "TEXT")
    private String description;
    
    /**
     * Особенности оборудования (расширенное описание)
     */
    @Column(columnDefinition = "TEXT")
    private String features;
    
    /**
     * Рекомендации по размещению
     */
    @Column(columnDefinition = "TEXT")
    private String placementInfo;
    
    /**
     * Информация о производителе
     */
    @Column(columnDefinition = "TEXT")
    private String producerInfo;
    
    /**
     * Список товаров (Product), которые ссылаются на эту детальную запись
     * Связь OneToMany: одна детальная запись может использоваться несколькими товарами
     */
    @OneToMany(mappedBy = "minerDetail", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Product> products = new ArrayList<>();
    
    /**
     * Время создания записи
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Время последнего обновления
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

---

### Шаг 2: Обновление Entity `Product`

**Файл:** `shop-backend/src/main/java/com/miners/shop/entity/Product.java`

**Добавить поле для связи с MinerDetail:**

```java
/**
 * Связь с детальной информацией о майнере (редактируемые данные)
 * ManyToOne: несколько товаров могут ссылаться на одну детальную запись (объединение)
 */
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "miner_detail_id", nullable = true)
private MinerDetail minerDetail;
```

**Полный обновленный класс:**

```java
package com.miners.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Сущность товара (майнера)
 * Один товар может иметь несколько предложений от разных продавцов
 * Связан с MinerDetail для отображения редактируемых данных на сайте
 */
@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Модель майнера (например: S19j PRO 104T, S21 200T)
     * Исходные данные от нейросети - не редактируются
     */
    @Column(nullable = false, length = 200, unique = true)
    private String model;
    
    /**
     * Описание товара (опционально)
     */
    @Column(columnDefinition = "TEXT")
    private String description;
    
    /**
     * Производитель майнера (Bitmain, MicroBT, Canaan, Avalon, Innosilicon и т.д.)
     * Исходные данные от нейросети - не редактируются напрямую
     * Для редактирования используйте MinerDetail.manufacturer
     */
    @Column(length = 100)
    private String manufacturer;
    
    /**
     * Связь с детальной информацией о майнере (редактируемые данные)
     * ManyToOne: несколько товаров могут ссылаться на одну детальную запись (объединение)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "miner_detail_id", nullable = true)
    private MinerDetail minerDetail;
    
    /**
     * Список предложений для этого товара
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Offer> offers = new ArrayList<>();
    
    /**
     * Время создания записи
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Время последнего обновления
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Минимальная цена за единицу (transient - не сохраняется в БД, вычисляется на лету)
     */
    @Transient
    private BigDecimal minPrice;
    
    /**
     * URL изображения товара (transient - не сохраняется в БД)
     */
    @Transient
    private String imageUrl;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

---

### Шаг 3: Создание Repository `MinerDetailRepository`

**Файл:** `shop-backend/src/main/java/com/miners/shop/repository/MinerDetailRepository.java`

```java
package com.miners.shop.repository;

import com.miners.shop.entity.MinerDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с детальной информацией о майнерах
 */
@Repository
public interface MinerDetailRepository extends JpaRepository<MinerDetail, Long> {
    
    /**
     * Найти детальную запись по стандартизированному названию
     */
    Optional<MinerDetail> findByStandardName(String standardName);
    
    /**
     * Найти детальные записи по производителю
     */
    List<MinerDetail> findByManufacturer(String manufacturer);
    
    /**
     * Найти детальные записи по серии
     */
    List<MinerDetail> findBySeries(String series);
    
    /**
     * Найти детальные записи по алгоритму
     */
    List<MinerDetail> findByAlgorithm(String algorithm);
    
    /**
     * Получить список уникальных производителей
     */
    @Query("SELECT DISTINCT md.manufacturer FROM MinerDetail md WHERE md.manufacturer IS NOT NULL AND md.manufacturer != '' ORDER BY md.manufacturer")
    List<String> findDistinctManufacturers();
    
    /**
     * Получить список уникальных серий
     */
    @Query("SELECT DISTINCT md.series FROM MinerDetail md WHERE md.series IS NOT NULL AND md.series != '' ORDER BY md.series")
    List<String> findDistinctSeries();
    
    /**
     * Получить список уникальных алгоритмов
     */
    @Query("SELECT DISTINCT md.algorithm FROM MinerDetail md WHERE md.algorithm IS NOT NULL AND md.algorithm != '' ORDER BY md.algorithm")
    List<String> findDistinctAlgorithms();
}
```

---

### Шаг 4: Создание Service `MinerDetailService`

**Файл:** `shop-backend/src/main/java/com/miners/shop/service/MinerDetailService.java`

```java
package com.miners.shop.service;

import com.miners.shop.entity.MinerDetail;
import com.miners.shop.entity.Product;
import com.miners.shop.repository.MinerDetailRepository;
import com.miners.shop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с детальной информацией о майнерах
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MinerDetailService {
    
    private final MinerDetailRepository minerDetailRepository;
    private final ProductRepository productRepository;
    
    /**
     * Создает детальную запись для товара с данными из нейросети
     * Вызывается автоматически при создании нового Product
     * 
     * @param product Товар, для которого создается детальная запись
     * @return Созданная детальная запись
     */
    @Transactional
    public MinerDetail createMinerDetailForProduct(Product product) {
        log.info("Создание детальной записи для товара: {} (ID: {})", product.getModel(), product.getId());
        
        MinerDetail minerDetail = new MinerDetail();
        
        // Заполняем стандартизированное название из model товара
        minerDetail.setStandardName(product.getModel());
        
        // Заполняем производителя, если он есть в товаре
        if (product.getManufacturer() != null && !product.getManufacturer().trim().isEmpty()) {
            minerDetail.setManufacturer(product.getManufacturer().trim());
        }
        
        // Пытаемся извлечь серию из названия модели
        // Например: "S19j PRO 104T" -> серия "S19j"
        String series = extractSeriesFromModel(product.getModel());
        if (series != null && !series.isEmpty()) {
            minerDetail.setSeries(series);
            log.debug("Извлечена серия из модели: {} -> серия: {}", product.getModel(), series);
        }
        
        // Остальные поля остаются пустыми и будут заполнены администратором или ИИ
        // Сохраняем детальную запись
        MinerDetail saved = minerDetailRepository.save(minerDetail);
        log.info("Создана детальная запись для товара {}: ID={}, стандартное название={}", 
                product.getModel(), saved.getId(), saved.getStandardName());
        
        // Устанавливаем связь в товаре
        product.setMinerDetail(saved);
        productRepository.save(product);
        
        return saved;
    }
    
    /**
     * Извлекает серию из названия модели
     * Логика: ищет паттерн вида "S19j", "L7", "S21" и т.д.
     * 
     * @param model Название модели
     * @return Серия или null
     */
    private String extractSeriesFromModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            return null;
        }
        
        // Удаляем лишние пробелы
        String trimmed = model.trim();
        
        // Паттерны для поиска серии:
        // - "S19j PRO 104T" -> "S19j"
        // - "L7 9050M" -> "L7"
        // - "S21 200T" -> "S21"
        
        // Ищем паттерн: буква + цифры + опционально буква
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^([A-Z]\\d+[a-z]?|[A-Z]\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(trimmed);
        
        if (matcher.find()) {
            String series = matcher.group(1);
            log.debug("Извлечена серия '{}' из модели '{}'", series, model);
            return series;
        }
        
        return null;
    }
    
    /**
     * Получает детальную запись по ID
     */
    @Transactional(readOnly = true)
    public Optional<MinerDetail> getMinerDetailById(Long id) {
        return minerDetailRepository.findById(id);
    }
    
    /**
     * Обновляет детальную запись
     */
    @Transactional
    public MinerDetail updateMinerDetail(MinerDetail minerDetail) {
        log.info("Обновление детальной записи: ID={}, стандартное название={}", 
                minerDetail.getId(), minerDetail.getStandardName());
        return minerDetailRepository.save(minerDetail);
    }
    
    /**
     * Объединяет несколько товаров, связав их с одной целевой детальной записью
     * 
     * Логика объединения:
     * 1. Выбирается целевая MinerDetail (в которую объединяются товары)
     * 2. Выбираются один или несколько Product (которые объединяются)
     * 3. У всех выбранных Product устанавливается minerDetailId = targetMinerDetailId
     * 
     * Пример:
     * - MinerDetail ID=1 (целевая, в которую объединяем)
     * - Product ID=2, ID=3, ID=5 (объединяемые товары)
     * - Результат: у Product ID=2, ID=3, ID=5 устанавливается minerDetailId = 1
     * 
     * @param targetMinerDetailId ID целевой детальной записи (в которую объединяются товары)
     * @param productIds Список ID товаров, которые объединяются в целевую детальную запись
     */
    @Transactional
    public void mergeProducts(Long targetMinerDetailId, List<Long> productIds) {
        log.info("Объединение товаров: {} товаров будут привязаны к целевой MinerDetail ID={}", 
                productIds.size(), targetMinerDetailId);
        
        // Проверяем, что целевая MinerDetail существует
        Optional<MinerDetail> targetMinerDetailOpt = minerDetailRepository.findById(targetMinerDetailId);
        if (targetMinerDetailOpt.isEmpty()) {
            throw new IllegalArgumentException("Целевая MinerDetail с ID=" + targetMinerDetailId + " не найдена");
        }
        
        MinerDetail targetMinerDetail = targetMinerDetailOpt.get();
        log.info("Целевая MinerDetail: ID={}, стандартное название={}", 
                targetMinerDetail.getId(), targetMinerDetail.getStandardName());
        
        // Устанавливаем выбранную целевую MinerDetail всем объединяемым товарам
        for (Long productId : productIds) {
            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                
                // Получаем текущую MinerDetail (если есть) для логирования
                MinerDetail previousMinerDetail = product.getMinerDetail();
                String previousInfo = previousMinerDetail != null 
                        ? "ID=" + previousMinerDetail.getId() + " (" + previousMinerDetail.getStandardName() + ")"
                        : "нет";
                
                // Устанавливаем целевую MinerDetail
                product.setMinerDetail(targetMinerDetail);
                productRepository.save(product);
                
                log.info("Товар ID={} ({}) объединен: {} -> MinerDetail ID={} ({})", 
                        productId, 
                        product.getModel(),
                        previousInfo,
                        targetMinerDetail.getId(),
                        targetMinerDetail.getStandardName());
            } else {
                log.warn("Товар с ID={} не найден, пропускаем", productId);
            }
        }
        
        log.info("Объединение товаров завершено: {} товаров привязаны к целевой MinerDetail ID={} ({})", 
                productIds.size(), 
                targetMinerDetailId,
                targetMinerDetail.getStandardName());
    }
    
    /**
     * Получает все детальные записи
     */
    @Transactional(readOnly = true)
    public List<MinerDetail> getAllMinerDetails() {
        return minerDetailRepository.findAll();
    }
    
    /**
     * Получает список уникальных производителей
     */
    @Transactional(readOnly = true)
    public List<String> getDistinctManufacturers() {
        return minerDetailRepository.findDistinctManufacturers();
    }
    
    /**
     * Получает список уникальных серий
     */
    @Transactional(readOnly = true)
    public List<String> getDistinctSeries() {
        return minerDetailRepository.findDistinctSeries();
    }
    
    /**
     * Получает список уникальных алгоритмов
     */
    @Transactional(readOnly = true)
    public List<String> getDistinctAlgorithms() {
        return minerDetailRepository.findDistinctAlgorithms();
    }
}
```

---

### Шаг 5: Интеграция с `ProductService`

**Файл:** `shop-backend/src/main/java/com/miners/shop/service/ProductService.java`

**Добавить зависимость и метод:**

```java
private final MinerDetailService minerDetailService;
```

**Обновить метод `processProduct`:**

После создания или получения `Product`, добавить создание `MinerDetail`:

```java
// После строки: Product product = productRepository.findByModel(model)...
// Если товар новый, создаем детальную запись
if (product.getId() == null || product.getMinerDetail() == null) {
    try {
        MinerDetail minerDetail = minerDetailService.createMinerDetailForProduct(product);
        product.setMinerDetail(minerDetail);
        log.info("Создана детальная запись для нового товара: {} -> MinerDetail ID={}", 
                product.getModel(), minerDetail.getId());
    } catch (Exception e) {
        log.error("Ошибка при создании детальной записи для товара {}: {}", 
                product.getModel(), e.getMessage(), e);
        // Не прерываем выполнение, если ошибка в создании детальной записи
    }
}
```

---

### Шаг 6: Создание DTO `MinerDetailDTO`

**Файл:** `shop-backend/src/main/java/com/miners/shop/dto/MinerDetailDTO.java`

```java
package com.miners.shop.dto;

import com.miners.shop.entity.MinerDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO для детальной информации о майнере
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MinerDetailDTO {
    
    private Long id;
    private String standardName;
    private String manufacturer;
    private String series;
    private String hashrate;
    private String algorithm;
    private String powerConsumption;
    private String coins;
    private String powerSource;
    private String cooling;
    private String operatingTemperature;
    private String dimensions;
    private String noiseLevel;
    private String description;
    private String features;
    private String placementInfo;
    private String producerInfo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Long> productIds; // ID товаров, связанных с этой детальной записью
    
    /**
     * Преобразует сущность в DTO
     */
    public static MinerDetailDTO fromEntity(MinerDetail minerDetail) {
        if (minerDetail == null) {
            return null;
        }
        
        MinerDetailDTO dto = MinerDetailDTO.builder()
                .id(minerDetail.getId())
                .standardName(minerDetail.getStandardName())
                .manufacturer(minerDetail.getManufacturer())
                .series(minerDetail.getSeries())
                .hashrate(minerDetail.getHashrate())
                .algorithm(minerDetail.getAlgorithm())
                .powerConsumption(minerDetail.getPowerConsumption())
                .coins(minerDetail.getCoins())
                .powerSource(minerDetail.getPowerSource())
                .cooling(minerDetail.getCooling())
                .operatingTemperature(minerDetail.getOperatingTemperature())
                .dimensions(minerDetail.getDimensions())
                .noiseLevel(minerDetail.getNoiseLevel())
                .description(minerDetail.getDescription())
                .features(minerDetail.getFeatures())
                .placementInfo(minerDetail.getPlacementInfo())
                .producerInfo(minerDetail.getProducerInfo())
                .createdAt(minerDetail.getCreatedAt())
                .updatedAt(minerDetail.getUpdatedAt())
                .build();
        
        // Добавляем ID связанных товаров
        if (minerDetail.getProducts() != null) {
            dto.setProductIds(minerDetail.getProducts().stream()
                    .map(product -> product.getId())
                    .toList());
        }
        
        return dto;
    }
    
    /**
     * Преобразует DTO в сущность (для создания/обновления)
     */
    public MinerDetail toEntity() {
        MinerDetail minerDetail = new MinerDetail();
        minerDetail.setId(this.id);
        minerDetail.setStandardName(this.standardName);
        minerDetail.setManufacturer(this.manufacturer);
        minerDetail.setSeries(this.series);
        minerDetail.setHashrate(this.hashrate);
        minerDetail.setAlgorithm(this.algorithm);
        minerDetail.setPowerConsumption(this.powerConsumption);
        minerDetail.setCoins(this.coins);
        minerDetail.setPowerSource(this.powerSource);
        minerDetail.setCooling(this.cooling);
        minerDetail.setOperatingTemperature(this.operatingTemperature);
        minerDetail.setDimensions(this.dimensions);
        minerDetail.setNoiseLevel(this.noiseLevel);
        minerDetail.setDescription(this.description);
        minerDetail.setFeatures(this.features);
        minerDetail.setPlacementInfo(this.placementInfo);
        minerDetail.setProducerInfo(this.producerInfo);
        return minerDetail;
    }
}
```

---

### Шаг 7: Создание Controller `MinerDetailController`

**Файл:** `shop-backend/src/main/java/com/miners/shop/controller/MinerDetailController.java`

```java
package com.miners.shop.controller;

import com.miners.shop.dto.MinerDetailDTO;
import com.miners.shop.entity.MinerDetail;
import com.miners.shop.entity.Product;
import com.miners.shop.repository.ProductRepository;
import com.miners.shop.service.MinerDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Контроллер для управления детальной информацией о майнерах
 */
@Controller
@RequestMapping("/miner-details")
@RequiredArgsConstructor
@Slf4j
public class MinerDetailController {
    
    private final MinerDetailService minerDetailService;
    private final ProductRepository productRepository;
    
    /**
     * Страница со списком всех детальных записей (таблица с основными параметрами)
     * Отображает таблицу с 4-5 основными параметрами для быстрого просмотра
     * 
     * Основные параметры в таблице:
     * 1. Стандартизированное название (standardName)
     * 2. Производитель (manufacturer)
     * 3. Хэшрейт (hashrate)
     * 4. Алгоритм (algorithm)
     * 5. Потребление (powerConsumption)
     */
    @GetMapping
    public String list(Model model) {
        List<MinerDetail> minerDetails = minerDetailService.getAllMinerDetails();
        List<MinerDetailDTO> dtos = minerDetails.stream()
                .map(MinerDetailDTO::fromEntity)
                .collect(Collectors.toList());
        
        model.addAttribute("minerDetails", dtos);
        model.addAttribute("manufacturers", minerDetailService.getDistinctManufacturers());
        model.addAttribute("series", minerDetailService.getDistinctSeries());
        model.addAttribute("algorithms", minerDetailService.getDistinctAlgorithms());
        
        return "miner-details/list";
    }
    
    /**
     * Страница детального просмотра одного майнера (со всей информацией)
     * Отображает все данные MinerDetail: технические характеристики, описания, связанные товары
     */
    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        MinerDetail minerDetail = minerDetailService.getMinerDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("MinerDetail не найден: " + id));
        
        MinerDetailDTO dto = MinerDetailDTO.fromEntity(minerDetail);
        
        // Получаем список связанных товаров (Product)
        List<Product> linkedProducts = minerDetail.getProducts();
        
        model.addAttribute("minerDetail", dto);
        model.addAttribute("linkedProducts", linkedProducts);
        model.addAttribute("linkedProductsCount", linkedProducts != null ? linkedProducts.size() : 0);
        
        return "miner-details/view";
    }
    
    /**
     * Страница редактирования детальной записи (для администратора)
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        MinerDetail minerDetail = minerDetailService.getMinerDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("MinerDetail не найден: " + id));
        
        MinerDetailDTO dto = MinerDetailDTO.fromEntity(minerDetail);
        
        // Получаем список всех товаров для возможности объединения
        List<Product> allProducts = productRepository.findAll();
        
        model.addAttribute("minerDetail", dto);
        model.addAttribute("allProducts", allProducts);
        model.addAttribute("linkedProducts", minerDetail.getProducts());
        
        return "miner-details/edit";
    }
    
    /**
     * Обновление детальной записи
     */
    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute MinerDetailDTO dto) {
        MinerDetail minerDetail = dto.toEntity();
        minerDetail.setId(id);
        minerDetailService.updateMinerDetail(minerDetail);
        return "redirect:/miner-details/" + id + "?success=true";
    }
    
    /**
     * REST API: Получить детальную запись в JSON
     */
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<MinerDetailDTO> getJson(@PathVariable Long id) {
        return minerDetailService.getMinerDetailById(id)
                .map(MinerDetailDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * REST API: Обновить детальную запись
     */
    @PutMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateJson(@PathVariable Long id, @RequestBody MinerDetailDTO dto) {
        try {
            MinerDetail minerDetail = dto.toEntity();
            minerDetail.setId(id);
            MinerDetail updated = minerDetailService.updateMinerDetail(minerDetail);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Детальная запись успешно обновлена");
            response.put("data", MinerDetailDTO.fromEntity(updated));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка при обновлении детальной записи: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Ошибка при обновлении: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * REST API: Объединить товары
     * 
     * Объединяет выбранные товары в целевую детальную запись
     * 
     * Пример запроса:
     * POST /admin/miner-details/api/1/merge
     * {
     *   "productIds": [2, 3, 5]
     * }
     * 
     * Это означает: объединить Product ID=2, ID=3, ID=5 в целевую MinerDetail ID=1
     * 
     * @param id ID целевой MinerDetail (в которую объединяются товары)
     * @param request Тело запроса с полем productIds - список ID товаров для объединения
     * @return Результат операции
     */
    @PostMapping("/api/{id}/merge")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> mergeProducts(@PathVariable Long id, @RequestBody Map<String, List<Long>> request) {
        try {
            // Получаем список ID товаров из запроса
            List<Long> productIds = request.get("productIds");
            if (productIds == null || productIds.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Список ID товаров (productIds) обязателен");
                return ResponseEntity.badRequest().body(error);
            }
            
            log.info("Запрос на объединение {} товаров в целевую MinerDetail ID={}", productIds.size(), id);
            
            // Выполняем объединение: все выбранные товары привязываются к целевой MinerDetail
            minerDetailService.mergeProducts(id, productIds);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Товары успешно объединены в целевую MinerDetail");
            response.put("targetMinerDetailId", id);
            response.put("mergedProductIds", productIds);
            response.put("mergedCount", productIds.size());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Ошибка валидации при объединении товаров: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Ошибка при объединении товаров: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Ошибка при объединении: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
```

---

## Интеграция с существующим кодом

### Обновление `ProductsController` для использования `MinerDetail`

**Файл:** `shop-backend/src/main/java/com/miners/shop/controller/ProductsController.java`

**Обновить метод `productDetails`:**

При отображении детальной страницы товара, использовать данные из `MinerDetail` вместо `Product`:

```java
// Получаем товар
Optional<Product> productOpt = productService.getProductById(id);
Product product = productOpt.get();

// Получаем детальную информацию, если она есть
MinerDetail minerDetail = product.getMinerDetail();

// Используем данные из MinerDetail для отображения, если они есть
String displayName = minerDetail != null ? minerDetail.getStandardName() : product.getModel();
String displayManufacturer = minerDetail != null && minerDetail.getManufacturer() != null 
        ? minerDetail.getManufacturer() 
        : product.getManufacturer();

model.addAttribute("product", product);
model.addAttribute("minerDetail", minerDetail);
model.addAttribute("displayName", displayName);
model.addAttribute("displayManufacturer", displayManufacturer);
```

### Обновление шаблонов

**Файл:** `shop-backend/src/main/resources/templates/product-details-new.html`

**Использовать данные из `minerDetail` для отображения:**

```html
<!-- Используем стандартизированное название, если есть -->
<h1 th:text="${minerDetail != null ? minerDetail.standardName : product.model}">
    Название товара
</h1>

<!-- Используем производителя из детальной записи -->
<div th:if="${minerDetail != null && minerDetail.manufacturer != null}">
    <span th:text="${minerDetail.manufacturer}">Производитель</span>
</div>

<!-- Отображаем технические характеристики, если они заполнены -->
<div th:if="${minerDetail != null && minerDetail.hashrate != null}">
    <strong>Хэшрейт:</strong>
    <span th:text="${minerDetail.hashrate}">860 TH/s</span>
</div>

<div th:if="${minerDetail != null && minerDetail.algorithm != null}">
    <strong>Алгоритм:</strong>
    <span th:text="${minerDetail.algorithm}">SHA-256</span>
</div>

<!-- И так далее для остальных полей -->
```

---

## Примеры использования

### Пример 1: Автоматическое создание `MinerDetail` при создании `Product`

```java
// В ProductService.processProduct()
Product product = productRepository.findByModel(model)
        .orElseGet(() -> {
            Product newProduct = new Product();
            newProduct.setModel(model);
            newProduct.setManufacturer((String) productData.get("manufacturer"));
            return productRepository.save(newProduct);
        });

// Автоматически создаем детальную запись для нового товара
if (product.getMinerDetail() == null) {
    MinerDetail minerDetail = minerDetailService.createMinerDetailForProduct(product);
    product.setMinerDetail(minerDetail);
    productRepository.save(product);
}
```

### Пример 2: Обновление детальной записи администратором

```java
// В контроллере
MinerDetail minerDetail = minerDetailService.getMinerDetailById(1L).orElseThrow();

// Обновляем поля
minerDetail.setStandardName("Antminer S21E XP Hydro 3U");
minerDetail.setManufacturer("Bitmain");
minerDetail.setSeries("S21");
minerDetail.setHashrate("860 TH/s");
minerDetail.setAlgorithm("SHA-256");
minerDetail.setPowerConsumption("11180 Вт/ч");
minerDetail.setCoins("[\"BTC\", \"BCH\", \"BCV\"]");
minerDetail.setDescription("Мощный ASIC-майнер для добычи Bitcoin...");

// Сохраняем
minerDetailService.updateMinerDetail(minerDetail);
```

### Пример 3: Объединение нескольких товаров

```java
// Ситуация: у нас есть несколько товаров с похожими названиями, которые нужно объединить
// Товары из products:
// - Product ID=1, model="S19j PRO 104T", minerDetailId=1
// - Product ID=2, model="S19j PRO 104 TH/s", minerDetailId=2
// - Product ID=3, model="Bitmain S19j PRO 104T", minerDetailId=3

// Шаг 1: Выбираем целевую MinerDetail (в которую объединяем)
// Например, выбираем MinerDetail ID=1 как основную
MinerDetail targetMinerDetail = minerDetailService.getMinerDetailById(1L).orElseThrow();

// Шаг 2: Выбираем товары, которые нужно объединить
// Объединяем Product ID=2 и ID=3 в целевую MinerDetail ID=1
List<Long> productsToMerge = List.of(2L, 3L);

// Шаг 3: Выполняем объединение
// У Product ID=2 и ID=3 устанавливается minerDetailId = 1
minerDetailService.mergeProducts(1L, productsToMerge);

// Результат:
// - Product ID=1, minerDetailId=1 (не изменился)
// - Product ID=2, minerDetailId=1 (объединен в целевую)
// - Product ID=3, minerDetailId=1 (объединен в целевую)
// Все три товара теперь отображаются с одинаковыми детальными данными из MinerDetail ID=1
```

### Пример 4: Использование в шаблонах

```html
<!-- В product-details-new.html -->
<div th:if="${minerDetail != null}">
    <h1 th:text="${minerDetail.standardName}">Название</h1>
    <p th:text="${minerDetail.description}">Описание</p>
    
    <table>
        <tr th:if="${minerDetail.hashrate != null}">
            <td>Хэшрейт:</td>
            <td th:text="${minerDetail.hashrate}">860 TH/s</td>
        </tr>
        <tr th:if="${minerDetail.algorithm != null}">
            <td>Алгоритм:</td>
            <td th:text="${minerDetail.algorithm}">SHA-256</td>
        </tr>
        <tr th:if="${minerDetail.powerConsumption != null}">
            <td>Потребление:</td>
            <td th:text="${minerDetail.powerConsumption}">11180 Вт/ч</td>
        </tr>
    </table>
</div>
```

---

## Страницы просмотра

### Шаг 8: Создание шаблонов для страниц

#### Шаблон: `miner-details/list.html` - Таблица со списком майнеров

**Файл:** `shop-backend/src/main/resources/templates/miner-details/list.html`

**Основные параметры в таблице (4-5 колонок):**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="ru">
<head>
    <meta charset="UTF-8">
    <title>Детали майнеров - Таблица</title>
    <!-- Bootstrap, стили и т.д. -->
</head>
<body>
    <div class="container">
        <h1>Детали майнеров</h1>
        
        <!-- Фильтры (опционально) -->
        <div class="filters mb-3">
            <select id="manufacturerFilter" class="form-select">
                <option value="">Все производители</option>
                <option th:each="manufacturer : ${manufacturers}" 
                        th:value="${manufacturer}" 
                        th:text="${manufacturer}">
                </option>
            </select>
        </div>
        
        <!-- Таблица с основными параметрами -->
        <table class="table table-striped">
            <thead>
                <tr>
                    <th>Название</th>
                    <th>Производитель</th>
                    <th>Хэшрейт</th>
                    <th>Алгоритм</th>
                    <th>Потребление</th>
                    <th>Действия</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="detail : ${minerDetails}">
                    <!-- 1. Стандартизированное название -->
                    <td>
                        <a th:href="@{/miner-details/{id}(id=${detail.id})}" 
                           th:text="${detail.standardName}">
                            Название
                        </a>
                    </td>
                    
                    <!-- 2. Производитель -->
                    <td th:text="${detail.manufacturer} ?: '-'">Производитель</td>
                    
                    <!-- 3. Хэшрейт -->
                    <td th:text="${detail.hashrate} ?: '-'">Хэшрейт</td>
                    
                    <!-- 4. Алгоритм -->
                    <td th:text="${detail.algorithm} ?: '-'">Алгоритм</td>
                    
                    <!-- 5. Потребление -->
                    <td th:text="${detail.powerConsumption} ?: '-'">Потребление</td>
                    
                    <!-- Действия -->
                    <td>
                        <a th:href="@{/miner-details/{id}(id=${detail.id})}" 
                           class="btn btn-sm btn-primary">Просмотр</a>
                        <a th:href="@{/miner-details/{id}/edit(id=${detail.id})}" 
                           class="btn btn-sm btn-secondary">Редактировать</a>
                    </td>
                </tr>
            </tbody>
        </table>
        
        <!-- Пустое состояние -->
        <div th:if="${minerDetails == null || minerDetails.isEmpty()}" 
             class="alert alert-info">
            Детальные записи майнеров отсутствуют
        </div>
    </div>
</body>
</html>
```

#### Шаблон: `miner-details/view.html` - Детальный просмотр майнера

**Файл:** `shop-backend/src/main/resources/templates/miner-details/view.html`

**Отображает всю информацию о майнере:**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="ru">
<head>
    <meta charset="UTF-8">
    <title th:text="${minerDetail.standardName} + ' - Детали'">Детали майнера</title>
    <!-- Bootstrap, стили и т.д. -->
</head>
<body>
    <div class="container">
        <!-- Хлебные крошки -->
        <nav aria-label="breadcrumb">
            <ol class="breadcrumb">
                <li class="breadcrumb-item">
                    <a th:href="@{/miner-details}">Детали майнеров</a>
                </li>
                <li class="breadcrumb-item active" 
                    th:text="${minerDetail.standardName}">Название</li>
            </ol>
        </nav>
        
        <!-- Заголовок -->
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h1 th:text="${minerDetail.standardName}">Название майнера</h1>
            <div>
                <a th:href="@{/miner-details/{id}/edit(id=${minerDetail.id})}" 
                   class="btn btn-primary">Редактировать</a>
                <a th:href="@{/miner-details}" 
                   class="btn btn-secondary">Назад к списку</a>
            </div>
        </div>
        
        <!-- Основная информация -->
        <div class="row">
            <div class="col-md-8">
                <!-- Описание -->
                <div class="card mb-3" th:if="${minerDetail.description != null}">
                    <div class="card-header">
                        <h5>Описание</h5>
                    </div>
                    <div class="card-body">
                        <p th:utext="${minerDetail.description}">Описание майнера</p>
                    </div>
                </div>
                
                <!-- Технические характеристики -->
                <div class="card mb-3">
                    <div class="card-header">
                        <h5>Технические характеристики</h5>
                    </div>
                    <div class="card-body">
                        <table class="table table-bordered">
                            <tbody>
                                <!-- Хэшрейт -->
                                <tr th:if="${minerDetail.hashrate != null}">
                                    <td class="fw-bold" style="width: 30%;">Хэшрейт</td>
                                    <td th:text="${minerDetail.hashrate}">860 TH/s</td>
                                </tr>
                                
                                <!-- Алгоритм -->
                                <tr th:if="${minerDetail.algorithm != null}">
                                    <td class="fw-bold">Алгоритм</td>
                                    <td th:text="${minerDetail.algorithm}">SHA-256</td>
                                </tr>
                                
                                <!-- Потребление -->
                                <tr th:if="${minerDetail.powerConsumption != null}">
                                    <td class="fw-bold">Потребление энергии</td>
                                    <td th:text="${minerDetail.powerConsumption}">11180 Вт/ч</td>
                                </tr>
                                
                                <!-- Добываемые монеты -->
                                <tr th:if="${minerDetail.coins != null}">
                                    <td class="fw-bold">Добываемые монеты</td>
                                    <td th:text="${minerDetail.coins}">BTC/BCH/BCV</td>
                                </tr>
                                
                                <!-- Источник питания -->
                                <tr th:if="${minerDetail.powerSource != null}">
                                    <td class="fw-bold">Источник питания</td>
                                    <td th:text="${minerDetail.powerSource}">Интегрированный</td>
                                </tr>
                                
                                <!-- Охлаждение -->
                                <tr th:if="${minerDetail.cooling != null}">
                                    <td class="fw-bold">Охлаждение</td>
                                    <td th:text="${minerDetail.cooling}">Водяное</td>
                                </tr>
                                
                                <!-- Рабочая температура -->
                                <tr th:if="${minerDetail.operatingTemperature != null}">
                                    <td class="fw-bold">Рабочая температура</td>
                                    <td th:text="${minerDetail.operatingTemperature}">от 0 до 40 °С</td>
                                </tr>
                                
                                <!-- Размеры -->
                                <tr th:if="${minerDetail.dimensions != null}">
                                    <td class="fw-bold">Размеры</td>
                                    <td th:text="${minerDetail.dimensions}">900 x 486.2 x 132 мм</td>
                                </tr>
                                
                                <!-- Уровень шума -->
                                <tr th:if="${minerDetail.noiseLevel != null}">
                                    <td class="fw-bold">Уровень шума</td>
                                    <td th:text="${minerDetail.noiseLevel}">40 дБ</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
                
                <!-- Особенности оборудования -->
                <div class="card mb-3" th:if="${minerDetail.features != null}">
                    <div class="card-header">
                        <h5>Особенности оборудования</h5>
                    </div>
                    <div class="card-body">
                        <p th:utext="${minerDetail.features}">Особенности</p>
                    </div>
                </div>
                
                <!-- Рекомендации по размещению -->
                <div class="card mb-3" th:if="${minerDetail.placementInfo != null}">
                    <div class="card-header">
                        <h5>Где лучше разместить</h5>
                    </div>
                    <div class="card-body">
                        <p th:utext="${minerDetail.placementInfo}">Рекомендации по размещению</p>
                    </div>
                </div>
                
                <!-- Информация о производителе -->
                <div class="card mb-3" th:if="${minerDetail.producerInfo != null}">
                    <div class="card-header">
                        <h5>О производителе</h5>
                    </div>
                    <div class="card-body">
                        <p th:utext="${minerDetail.producerInfo}">Информация о производителе</p>
                    </div>
                </div>
            </div>
            
            <!-- Боковая панель -->
            <div class="col-md-4">
                <!-- Основная информация -->
                <div class="card mb-3">
                    <div class="card-header">
                        <h5>Основная информация</h5>
                    </div>
                    <div class="card-body">
                        <dl>
                            <dt>Производитель</dt>
                            <dd th:text="${minerDetail.manufacturer} ?: 'Не указан'">Bitmain</dd>
                            
                            <dt>Серия</dt>
                            <dd th:text="${minerDetail.series} ?: 'Не указана'">S21</dd>
                            
                            <dt>Связанных товаров</dt>
                            <dd th:text="${linkedProductsCount}">0</dd>
                        </dl>
                    </div>
                </div>
                
                <!-- Связанные товары (Product) -->
                <div class="card" th:if="${linkedProducts != null && !linkedProducts.isEmpty()}">
                    <div class="card-header">
                        <h5>Связанные товары</h5>
                        <small class="text-muted" th:text="'Всего: ' + ${linkedProductsCount}">Всего: 0</small>
                    </div>
                    <div class="card-body">
                        <ul class="list-group list-group-flush">
                            <li class="list-group-item" th:each="product : ${linkedProducts}">
                                <a th:href="@{/products/{id}(id=${product.id})}" 
                                   th:text="${product.model}">
                                    Модель товара
                                </a>
                            </li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    </div>
</body>
</html>
```

#### Обновление роутинга контроллера

**Важно:** Метод `view()` должен быть объявлен **ДО** метода `editForm()`, чтобы `/miner-details/{id}` не перехватывался как `/miner-details/{id}/edit`.

**Правильный порядок методов в контроллере:**

```java
@GetMapping                    // GET /miner-details - список
public String list(...)

@GetMapping("/{id}")          // GET /miner-details/{id} - просмотр (должен быть ПЕРЕД editForm!)
public String view(...)

@GetMapping("/{id}/edit")     // GET /miner-details/{id}/edit - редактирование
public String editForm(...)
```

---

## Эндпоинты для страниц

### Сводная таблица эндпоинтов:

| Метод | Путь | Описание | Шаблон |
|-------|------|----------|--------|
| `GET` | `/miner-details` | Список всех майнеров (таблица с основными параметрами) | `miner-details/list.html` |
| `GET` | `/miner-details/{id}` | Детальный просмотр майнера (вся информация) | `miner-details/view.html` |
| `GET` | `/miner-details/{id}/edit` | Редактирование майнера (для администратора) | `miner-details/edit.html` |
| `POST` | `/miner-details/{id}` | Сохранение изменений | Редирект на `/miner-details/{id}` |
| `GET` | `/miner-details/api/{id}` | Получение данных в JSON | JSON ответ |
| `PUT` | `/miner-details/api/{id}` | Обновление через API | JSON ответ |
| `POST` | `/miner-details/api/{id}/merge` | Объединение товаров | JSON ответ |

---

## Тестирование

### Миграция базы данных

При использовании Hibernate с `ddl-auto: update`, таблица `miner_details` будет создана автоматически при первом запуске приложения после добавления Entity.

### Ручное тестирование

1. **Создание детальной записи:**
   - Создайте новый `Product` через webhook
   - Проверьте, что автоматически создался `MinerDetail`
   - Проверьте, что `product.minerDetail` установлен

2. **Редактирование детальной записи:**
   - Откройте страницу `/admin/miner-details/{id}/edit`
   - Измените поля (standardName, hashrate, и т.д.)
   - Сохраните
   - Проверьте, что изменения сохранились

3. **Объединение товаров:**
   - Создайте несколько `Product` с похожими названиями
   - Объедините их через API `/admin/miner-details/api/{id}/merge`
   - Проверьте, что все товары теперь ссылаются на один `MinerDetail`

4. **Страница списка деталей майнеров:**
   - Откройте страницу `/miner-details`
   - Проверьте, что отображается таблица с 4-5 основными параметрами
   - Проверьте, что есть ссылки на детальный просмотр и редактирование

5. **Страница детального просмотра:**
   - Откройте страницу `/miner-details/{id}` для существующего майнера
   - Проверьте, что отображаются все поля MinerDetail
   - Проверьте, что отображается список связанных товаров (Product)
   - Проверьте, что есть кнопка "Редактировать"

6. **Отображение на сайте:**
   - Откройте детальную страницу товара `/products/{id}`
   - Проверьте, что используются данные из `MinerDetail`, если они есть
   - Проверьте, что используются данные из `Product`, если `MinerDetail` отсутствует

---

*Документация создана: 2024-01-15*
*Последнее обновление: 2024-01-15*

