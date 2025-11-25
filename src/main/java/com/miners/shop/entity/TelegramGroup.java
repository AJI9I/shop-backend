package com.miners.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "telegram_groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelegramGroup {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String chatId;
    
    @Column(nullable = false, length = 500)
    private String chatName;
    
    /**
     * Статус мониторинга группы: true - мониторинг включен, false - выключен
     */
    @Column(nullable = false)
    private Boolean monitoringEnabled = true;
    
    /**
     * Количество сообщений из этой группы
     */
    @Column(nullable = false)
    private Long messageCount = 0L;
    
    /**
     * Дата последнего сообщения из этой группы
     */
    private LocalDateTime lastMessageDate;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
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


