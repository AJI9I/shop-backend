package com.miners.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Сущность для логирования 404 ошибок
 * Фиксирует на какие страницы и когда приходят запросы 404
 */
@Entity
@Table(name = "not_found_errors", indexes = {
    @Index(name = "idx_url", columnList = "url"),
    @Index(name = "idx_last_occurred", columnList = "lastOccurred")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotFoundError {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * URL, на который пришел запрос 404
     */
    @Column(nullable = false, length = 1000)
    private String url;
    
    /**
     * HTTP метод запроса (GET, POST и т.д.)
     */
    @Column(length = 10)
    private String httpMethod;
    
    /**
     * User-Agent браузера
     */
    @Column(length = 500)
    private String userAgent;
    
    /**
     * IP адрес клиента
     */
    @Column(length = 50)
    private String ipAddress;
    
    /**
     * Referer (откуда пришел запрос)
     */
    @Column(length = 1000)
    private String referer;
    
    /**
     * Количество запросов на этот URL
     * Используется для группировки одинаковых запросов
     */
    @Column(nullable = false)
    private Integer count = 1;
    
    /**
     * Время первого запроса
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime firstOccurred;
    
    /**
     * Время последнего запроса
     */
    @Column(nullable = false)
    private LocalDateTime lastOccurred;
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        firstOccurred = now;
        lastOccurred = now;
    }
    
    @PreUpdate
    protected void onUpdate() {
        lastOccurred = LocalDateTime.now();
    }
}

