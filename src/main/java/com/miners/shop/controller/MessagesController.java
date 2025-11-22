package com.miners.shop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miners.shop.entity.WhatsAppMessage;
import com.miners.shop.service.WhatsAppMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Контроллер для просмотра сообщений WhatsApp и обработанных данных от нейросети
 */
@Controller
@RequestMapping("/messages")
@RequiredArgsConstructor
@Slf4j
public class MessagesController {
    
    private final WhatsAppMessageService messageService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Страница со списком всех сообщений
     */
    @GetMapping
    public String messagesList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<WhatsAppMessage> messages = messageService.getAllMessages(pageable);
        
        model.addAttribute("messages", messages);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", messages.getTotalPages());
        model.addAttribute("totalElements", messages.getTotalElements());
        
        return "messages";
    }
    
    /**
     * Страница просмотра конкретного сообщения с оригинальным текстом и обработанными данными
     */
    @GetMapping("/{id}")
    public String viewMessage(@PathVariable Long id, Model model) {
        Optional<WhatsAppMessage> messageOpt = messageService.getMessageById(id);
        
        if (messageOpt.isEmpty()) {
            model.addAttribute("error", "Сообщение не найдено");
            return "message-view";
        }
        
        WhatsAppMessage message = messageOpt.get();
        model.addAttribute("message", message);
        
        // Парсим parsedData из JSON строки для красивого отображения
        Object parsedData = null;
        String parsedDataJson = null;
        if (message.getParsedData() != null && !message.getParsedData().trim().isEmpty()) {
            parsedData = messageService.parseParsedData(message.getParsedData());
            try {
                // Форматируем JSON для красивого отображения
                parsedDataJson = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(parsedData);
            } catch (Exception e) {
                log.error("Ошибка при форматировании parsedData JSON: {}", e.getMessage(), e);
                parsedDataJson = message.getParsedData(); // Используем исходный текст
            }
        }
        
        model.addAttribute("parsedData", parsedData);
        model.addAttribute("parsedDataJson", parsedDataJson);
        
        // Форматируем оригинальное сообщение для JSON отображения
        Map<String, Object> originalMessageJson = new HashMap<>();
        originalMessageJson.put("messageId", message.getMessageId());
        originalMessageJson.put("chatId", message.getChatId());
        originalMessageJson.put("chatName", message.getChatName());
        originalMessageJson.put("chatType", message.getChatType());
        originalMessageJson.put("senderId", message.getSenderId());
        originalMessageJson.put("senderName", message.getSenderName());
        originalMessageJson.put("senderPhoneNumber", message.getSenderPhoneNumber());
        originalMessageJson.put("content", message.getContent());
        originalMessageJson.put("timestamp", message.getTimestamp() != null ? message.getTimestamp().toString() : null);
        originalMessageJson.put("hasMedia", message.getHasMedia());
        originalMessageJson.put("messageType", message.getMessageType());
        originalMessageJson.put("isForwarded", message.getIsForwarded());
        originalMessageJson.put("isUpdate", message.getIsUpdate());
        originalMessageJson.put("originalMessageId", message.getOriginalMessageId());
        
        try {
            String originalMessageJsonString = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(originalMessageJson);
            model.addAttribute("originalMessageJson", originalMessageJsonString);
        } catch (Exception e) {
            log.error("Ошибка при форматировании оригинального сообщения: {}", e.getMessage(), e);
            model.addAttribute("originalMessageJson", "{}");
        }
        
        return "message-view";
    }
    
    /**
     * API эндпоинт для получения сообщения по ID в формате JSON
     */
    @GetMapping(value = "/{id}/json", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    @ResponseBody
    public ResponseEntity<?> getMessageJson(@PathVariable Long id) {
        Optional<WhatsAppMessage> messageOpt = messageService.getMessageById(id);
        
        if (messageOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        WhatsAppMessage message = messageOpt.get();
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", message.getId());
        response.put("messageId", message.getMessageId());
        response.put("chatId", message.getChatId());
        response.put("chatName", message.getChatName());
        response.put("chatType", message.getChatType());
        response.put("senderId", message.getSenderId());
        response.put("senderName", message.getSenderName());
        response.put("senderPhoneNumber", message.getSenderPhoneNumber());
        response.put("content", message.getContent());
        response.put("timestamp", message.getTimestamp() != null ? message.getTimestamp().toString() : null);
        response.put("hasMedia", message.getHasMedia());
        response.put("messageType", message.getMessageType());
        response.put("isForwarded", message.getIsForwarded());
        response.put("isUpdate", message.getIsUpdate());
        response.put("originalMessageId", message.getOriginalMessageId());
        response.put("createdAt", message.getCreatedAt() != null ? message.getCreatedAt().toString() : null);
        response.put("updatedAt", message.getUpdatedAt() != null ? message.getUpdatedAt().toString() : null);
        
        // Добавляем обработанные данные от нейросети
        if (message.getParsedData() != null && !message.getParsedData().trim().isEmpty()) {
            Object parsedData = messageService.parseParsedData(message.getParsedData());
            response.put("parsedData", parsedData);
            response.put("parsedDataRaw", message.getParsedData());
        } else {
            response.put("parsedData", null);
            response.put("parsedDataRaw", null);
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
}
