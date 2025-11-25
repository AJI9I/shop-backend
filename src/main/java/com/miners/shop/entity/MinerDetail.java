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
     * URL изображения майнера
     * Если указан, используется это изображение, иначе используется ImageUrlResolver
     */
    @Column(length = 500)
    private String imageUrl;
    
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

