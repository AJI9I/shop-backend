package com.miners.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String messageId;
    
    @Column(nullable = false)
    private String chatId;
    
    @Column(nullable = false, length = 500)
    private String chatName;
    
    @Column(nullable = false)
    private String chatType; // group или personal
    
    @Column(nullable = false)
    private String senderId;
    
    @Column(nullable = false, length = 200)
    private String senderName;
    
    private String senderPhoneNumber;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    private Boolean hasMedia = false;
    
    private String mediaMimetype;
    
    private String mediaFilename;
    
    @Column(columnDefinition = "TEXT")
    private String mediaData; // base64
    
    private String messageType;
    
    private Boolean isForwarded = false;
    
    /**
     * Флаг, указывающий что это сообщение является обновлением предыдущего сообщения от того же продавца
     */
    private Boolean isUpdate = false;
    
    /**
     * ID оригинального сообщения, которое было обновлено (для отслеживания истории обновлений)
     */
    @Column(length = 200)
    private String originalMessageId;
    
    /**
     * Обработанные данные от нейросети Ollama (JSON в формате TEXT)
     */
    @Column(columnDefinition = "TEXT")
    private String parsedData;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
