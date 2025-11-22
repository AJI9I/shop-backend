package com.miners.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Сущность заявки клиента на предложение
 * Связывает клиента с предложением (Offer) и исходным сообщением (WhatsAppMessage)
 */
@Entity
@Table(name = "requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Request {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Связь с предложением
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;
    
    /**
     * Связь с исходным сообщением WhatsApp (опционально)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "whatsapp_message_id", nullable = true)
    private WhatsAppMessage whatsAppMessage;
    
    /**
     * Имя клиента, оставившего заявку
     */
    @Column(nullable = false, length = 200)
    private String clientName;
    
    /**
     * Телефон клиента
     */
    @Column(nullable = false, length = 50)
    private String clientPhone;
    
    /**
     * Сообщение от клиента (опционально)
     */
    @Column(columnDefinition = "TEXT")
    private String message;
    
    /**
     * Статус заявки: NEW, PROCESSED, CLOSED, CANCELLED
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.NEW;
    
    /**
     * Комментарий администратора (опционально)
     */
    @Column(columnDefinition = "TEXT")
    private String adminComment;
    
    /**
     * Время создания заявки
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
    
    /**
     * Статусы заявки
     */
    public enum RequestStatus {
        NEW,           // Новая заявка
        PROCESSED,     // В обработке
        CLOSED,        // Закрыта
        CANCELLED      // Отменена
    }
}


