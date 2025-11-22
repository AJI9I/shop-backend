package com.miners.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Сущность продавца
 * Хранит информацию о продавцах для обратной связи
 */
@Entity
@Table(name = "sellers", uniqueConstraints = {
    @UniqueConstraint(columnNames = "phone")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Seller {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Имя продавца из WhatsApp
     */
    @Column(nullable = false, length = 200)
    private String name;
    
    /**
     * Телефон продавца (уникальный идентификатор)
     */
    @Column(nullable = false, unique = true, length = 50)
    private String phone;
    
    /**
     * ID продавца из WhatsApp (senderId)
     */
    @Column(length = 200)
    private String whatsappId;
    
    /**
     * Дополнительная контактная информация (опционально)
     */
    @Column(columnDefinition = "TEXT")
    private String contactInfo;
    
    /**
     * Рейтинг продавца (для будущего функционала)
     */
    private Double rating;
    
    /**
     * Количество сделок (для будущего функционала)
     */
    private Integer dealsCount = 0;
    
    /**
     * Список предложений от этого продавца
     */
    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
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

