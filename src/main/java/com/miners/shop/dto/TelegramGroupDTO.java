package com.miners.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelegramGroupDTO {
    
    private Long id;
    private String chatId;
    private String chatName;
    private Boolean monitoringEnabled;
    private Long messageCount;
    private LocalDateTime lastMessageDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


