package com.miners.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Сущность для хранения редиректов (301, 302)
 * Используется для управления редиректами со старых URL на новые
 */
@Entity
@Table(name = "redirects", uniqueConstraints = {
    @UniqueConstraint(columnNames = "fromUrl")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Redirect {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Старый URL (откуда редиректим)
     * Должен быть уникальным
     */
    @Column(nullable = false, unique = true, length = 500)
    private String fromUrl;
    
    /**
     * Новый URL (куда редиректим)
     */
    @Column(nullable = false, length = 500)
    private String toUrl;
    
    /**
     * Тип редиректа: 301 (постоянный) или 302 (временный)
     */
    @Column(nullable = false)
    private Integer redirectType = 301; // По умолчанию 301
    
    /**
     * Активен ли редирект
     */
    @Column(nullable = false)
    private Boolean active = true;
    
    /**
     * Время создания
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





