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
     * Заполняется из Ollama при первом предложении, если еще не заполнено
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

