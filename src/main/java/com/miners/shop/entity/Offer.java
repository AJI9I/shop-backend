package com.miners.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Сущность предложения о продаже товара
 * Один товар может иметь несколько предложений от разных продавцов
 */
@Entity
@Table(name = "offers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Offer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Связь с товаром
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    /**
     * Связь с продавцом
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seller_id", nullable = true) // Временно nullable для миграции
    private Seller seller;
    
    /**
     * Тип операции: SELL (продажа) или BUY (покупка)
     */
    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private OperationType operationType = OperationType.SELL;
    
    /**
     * Цена за единицу
     * Может быть null для запросов на покупку (BUY)
     */
    @Column(nullable = true, precision = 10, scale = 2)
    private BigDecimal price;
    
    /**
     * Валюта (u, руб, $ и т.д.)
     */
    @Column(length = 10)
    private String currency = "u";
    
    /**
     * Количество товара
     */
    @Column(nullable = false)
    private Integer quantity;
    
    /**
     * Состояние товара (НОВЫЙ, Б/У, БУ и т.д.)
     */
    @Column(length = 50)
    private String condition;
    
    /**
     * Дополнительные условия (например: "от 20шт", "лотом", "на прошивке без DevFee")
     */
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    /**
     * Локация продажи (Москва, Регион и т.д.)
     */
    @Column(length = 100)
    private String location;
    
    /**
     * Производитель майнера (Bitmain, MicroBT, Canaan и т.д.)
     */
    @Column(length = 100)
    private String manufacturer;
    
    /**
     * Мощность майнера (hashrate) - например, "104TH/s", "190TH/s", "106TH/s"
     */
    @Column(length = 50)
    private String hashrate;
    
    /**
     * Продавец (имя из WhatsApp) - оставляем для обратной совместимости
     * @deprecated Используйте seller.name вместо этого
     */
    @Deprecated
    @Column(length = 200)
    private String sellerName;
    
    /**
     * Телефон продавца - оставляем для обратной совместимости
     * @deprecated Используйте seller.phone вместо этого
     */
    @Deprecated
    @Column(length = 50)
    private String sellerPhone;
    
    /**
     * ID сообщения из WhatsApp, откуда получено предложение
     */
    @Column(length = 200)
    private String sourceMessageId;
    
    /**
     * Название чата, откуда получено предложение
     */
    @Column(length = 500)
    private String sourceChatName;
    
    /**
     * Дополнительные данные из Ollama в формате JSON
     * Содержит все поля, которые не обрабатываются отдельно
     */
    @Column(columnDefinition = "TEXT")
    private String additionalData;
    
    /**
     * Время создания предложения
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

