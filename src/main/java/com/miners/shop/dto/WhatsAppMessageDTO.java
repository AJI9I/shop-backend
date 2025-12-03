package com.miners.shop.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppMessageDTO {
    
    @NotBlank(message = "messageId обязателен")
    private String messageId;
    
    @NotBlank(message = "chatId обязателен")
    private String chatId;
    
    @NotBlank(message = "chatName обязателен")
    private String chatName;
    
    private String chatType;
    
    @NotBlank(message = "senderId обязателен")
    private String senderId;
    
    private String senderName;
    
    private String senderPhoneNumber;
    
    private String content;
    
    private String timestamp;
    
    private Boolean hasMedia;
    
    private String mediaMimetype;
    
    private String mediaFilename;
    
    private String mediaData;
    
    private String messageType;
    
    private Boolean isForwarded;
    
    /**
     * Распарсенные данные от Ollama (JSON объект)
     */
    private Object parsedData;
    
    /**
     * Счетчик дубликатов - количество раз, когда от этого отправителя было получено
     * идентичное сообщение в других группах за последние 10 минут
     */
    private Integer duplicateCount;
}
