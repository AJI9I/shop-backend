package com.miners.shop.controller;

import com.miners.shop.dto.WhatsAppMessageDTO;
import com.miners.shop.entity.WhatsAppMessage;
import com.miners.shop.service.ProductService;
import com.miners.shop.service.TelegramGroupService;
import com.miners.shop.service.WhatsAppMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {
    
    private final WhatsAppMessageService messageService;
    private final ProductService productService;
    private final TelegramGroupService groupService;
    
    /**
     * Endpoint для приема сообщений от WhatsApp сервиса
     */
    @PostMapping(value = "/whatsapp", produces = "application/json;charset=UTF-8", 
                 consumes = "application/json;charset=UTF-8")
    public ResponseEntity<?> receiveWhatsAppMessage(
            @Valid @RequestBody WhatsAppMessageDTO messageDTO,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        
        // ═══════════════════════════════════════════════════════════════════════════════
        // ДЕТАЛЬНОЕ ЛОГИРОВАНИЕ ПЕРЕДАЧИ ДАННЫХ ОТ WHATSAPP SERVICE
        // ═══════════════════════════════════════════════════════════════════════════════
        log.info("═".repeat(100));
        log.info("🚀🚀🚀 ПОЛУЧЕН ЗАПРОС ОТ WHATSAPP SERVICE 🚀🚀🚀");
        log.info("═".repeat(100));
        log.info("📥 ВРЕМЯ ПОЛУЧЕНИЯ: {}", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        log.info("═".repeat(100));
        
        // Логируем все поля messageDTO
        log.info("📋 ПОЛНАЯ ИНФОРМАЦИЯ О СООБЩЕНИИ:");
        log.info("   messageId: {}", messageDTO.getMessageId());
        log.info("   chatId: {}", messageDTO.getChatId());
        log.info("   chatName: {}", messageDTO.getChatName());
        log.info("   chatType: {}", messageDTO.getChatType());
        log.info("   senderId: {}", messageDTO.getSenderId());
        log.info("   senderName: {}", messageDTO.getSenderName());
        log.info("   senderPhoneNumber: {}", messageDTO.getSenderPhoneNumber());
        log.info("   content (первые 200 символов): {}", messageDTO.getContent() != null ? 
                messageDTO.getContent().substring(0, Math.min(200, messageDTO.getContent().length())) : "null");
        log.info("   timestamp: {}", messageDTO.getTimestamp());
        log.info("   hasMedia: {}", messageDTO.getHasMedia());
        log.info("   messageType: {}", messageDTO.getMessageType());
        log.info("   isForwarded: {}", messageDTO.getIsForwarded());
        
        // Логируем информацию о parsedData
        log.info("═".repeat(100));
        log.info("📦 ИНФОРМАЦИЯ О PARSED DATA:");
        if (messageDTO.getParsedData() == null) {
            log.warn("   ❌❌❌ PARSED DATA == NULL ❌❌❌");
            log.warn("   ⚠️  Распарсенные данные от Ollama отсутствуют!");
        } else {
            log.info("   ✅ parsedData присутствует");
            log.info("   Тип parsedData: {}", messageDTO.getParsedData().getClass().getName());
            
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String parsedDataJson = objectMapper.writeValueAsString(messageDTO.getParsedData());
                log.info("   📄 parsedData (JSON, полный):");
                log.info("   {}", parsedDataJson);
                
                if (messageDTO.getParsedData() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> parsedDataMap = (Map<String, Object>) messageDTO.getParsedData();
                    log.info("   Структура parsedData:");
                    log.info("   - Ключи: {}", parsedDataMap.keySet());
                    log.info("   - operationType: {}", parsedDataMap.get("operationType"));
                    log.info("   - location: {}", parsedDataMap.get("location"));
                    log.info("   - isMiningEquipment: {}", parsedDataMap.get("isMiningEquipment"));
                    
                    Object productsObj = parsedDataMap.get("products");
                    if (productsObj == null) {
                        log.warn("   ❌ products == null");
                    } else if (productsObj instanceof java.util.List) {
                        @SuppressWarnings("unchecked")
                        java.util.List<Map<String, Object>> products = (java.util.List<Map<String, Object>>) productsObj;
                        log.info("   ✅ products - список, размер: {}", products.size());
                        if (products.isEmpty()) {
                            log.warn("   ❌ products - ПУСТОЙ СПИСОК!");
                        } else {
                            log.info("   📦 Детали товаров:");
                            for (int i = 0; i < products.size(); i++) {
                                Map<String, Object> p = products.get(i);
                                log.info("   Товар {}: model={}, price={}, quantity={}, condition={}, location={}", 
                                        i + 1,
                                        p.get("model"),
                                        p.get("price"),
                                        p.get("quantity"),
                                        p.get("condition"),
                                        p.get("location"));
                            }
                        }
                    } else {
                        log.warn("   ❌ products не является List, тип: {}", productsObj.getClass().getName());
                    }
                } else {
                    log.warn("   ❌ parsedData не является Map, тип: {}", messageDTO.getParsedData().getClass().getName());
                }
            } catch (Exception e) {
                log.error("   ❌ Ошибка при обработке parsedData: {}", e.getMessage(), e);
            }
        }
        
        // Логируем информацию о sellerPhone
        log.info("═".repeat(100));
        log.info("📞 ИНФОРМАЦИЯ О НОМЕРЕ ТЕЛЕФОНА ПРОДАВЦА:");
        String originalSellerPhone = messageDTO.getSenderPhoneNumber();
        log.info("   senderPhoneNumber (оригинальный): {}", originalSellerPhone);
        log.info("   senderPhoneNumber == null: {}", originalSellerPhone == null);
        if (originalSellerPhone != null) {
            log.info("   senderPhoneNumber.length(): {}", originalSellerPhone.length());
            log.info("   senderPhoneNumber.contains(\"@\"): {}", originalSellerPhone.contains("@"));
            log.info("   senderPhoneNumber.contains(\"_\"): {}", originalSellerPhone.contains("_"));
            log.info("   senderPhoneNumber.matches(\"^[0-9]+$\"): {}", originalSellerPhone.matches("^[0-9]+$"));
            log.info("   senderPhoneNumber.length() > 15: {}", originalSellerPhone.length() > 15);
            
            // Проверка валидности
            boolean isValidPhone = originalSellerPhone.length() <= 15 
                    && !originalSellerPhone.contains("@") 
                    && !originalSellerPhone.contains("_") 
                    && originalSellerPhone.matches("^[0-9]+$");
            if (isValidPhone) {
                log.info("   ✅ Номер телефона ВАЛИДНЫЙ");
            } else {
                log.warn("   ❌ Номер телефона НЕВАЛИДНЫЙ (будет отклонен при создании продавца)");
            }
        } else {
            log.warn("   ❌ senderPhoneNumber == null (продавец не будет создан!)");
        }
        log.info("   senderId: {}", messageDTO.getSenderId());
        
        log.info("═".repeat(100));
        log.info("🚀 НАЧАЛО ОБРАБОТКИ СООБЩЕНИЯ");
        log.info("═".repeat(100));
        
        // Логируем входящие данные для диагностики кодировки
        log.info("Получен webhook от WhatsApp сервиса: messageId={}", messageDTO.getMessageId());
        if (messageDTO.getChatName() != null) {
            byte[] chatNameBytes = messageDTO.getChatName().getBytes(StandardCharsets.UTF_8);
            log.debug("chatName (UTF-8 bytes): {}", java.util.Arrays.toString(chatNameBytes));
            log.info("chatName (as received): '{}'", messageDTO.getChatName());
            log.info("chatName (length): {}", messageDTO.getChatName().length());
            log.info("chatName (contains Cyrillic): {}", messageDTO.getChatName().matches(".*[А-Яа-яЁё].*"));
        }
        if (messageDTO.getContent() != null) {
            String preview = messageDTO.getContent().substring(0, Math.min(100, messageDTO.getContent().length()));
            log.debug("content preview (first 100): {}", preview);
            log.debug("content (contains Cyrillic): {}", messageDTO.getContent().matches(".*[А-Яа-яЁё].*"));
        }
        
        try {
            // Проверяем, есть ли предыдущие сообщения от этого продавца (для определения обновлений)
            String originalMessageId = null;
            if (messageDTO.getSenderPhoneNumber() != null && messageDTO.getParsedData() != null) {
                originalMessageId = messageService.findPreviousMessageIdFromSeller(
                        messageDTO.getSenderPhoneNumber(), 
                        messageDTO.getChatId(),
                        messageDTO.getMessageId()
                );
            }
            
            // Сохраняем сообщение с информацией об обновлении
            WhatsAppMessage saved = messageService.saveMessage(messageDTO, originalMessageId);
            
            // Если это сообщение из группы, синхронизируем информацию о группе
            if ("group".equals(messageDTO.getChatType()) && messageDTO.getChatId() != null) {
                try {
                    groupService.syncGroupsFromMessages();
                } catch (Exception e) {
                    log.warn("Ошибка при синхронизации групп: {}", e.getMessage());
                }
            }
            
            // Обрабатываем распарсенные данные от Ollama, если они есть
            boolean isUpdate = false;
            log.info("═".repeat(80));
            log.info("ПРОВЕРКА PARSED DATA ДЛЯ СООБЩЕНИЯ: {}", messageDTO.getMessageId());
            log.info("═".repeat(80));
            
            if (messageDTO.getParsedData() == null) {
                log.warn("⚠️  parsedData == null - распарсенные данные от Ollama отсутствуют!");
            } else {
                log.info("✅ parsedData присутствует, тип: {}", messageDTO.getParsedData().getClass().getName());
                
                // Логируем содержимое parsedData
                try {
                    com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    String parsedDataJson = objectMapper.writeValueAsString(messageDTO.getParsedData());
                    log.info("📋 Содержимое parsedData (JSON):");
                    log.info(parsedDataJson);
                    
                    // Проверяем структуру
                    if (messageDTO.getParsedData() instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> parsedDataMap = (Map<String, Object>) messageDTO.getParsedData();
                        log.info("📋 parsedData является Map, ключи: {}", parsedDataMap.keySet());
                        log.info("📋 operationType: {}", parsedDataMap.get("operationType"));
                        log.info("📋 location: {}", parsedDataMap.get("location"));
                        
                        Object productsObj = parsedDataMap.get("products");
                        if (productsObj instanceof java.util.List) {
                            @SuppressWarnings("unchecked")
                            java.util.List<Map<String, Object>> products = (java.util.List<Map<String, Object>>) productsObj;
                            log.info("📋 products - список, размер: {}", products.size());
                        } else {
                            log.warn("⚠️  products не является List, тип: {}", productsObj != null ? productsObj.getClass().getName() : "null");
                        }
                    } else {
                        log.warn("⚠️  parsedData не является Map, тип: {}", messageDTO.getParsedData().getClass().getName());
                    }
                } catch (Exception e) {
                    log.error("❌ Ошибка при логировании parsedData: {}", e.getMessage(), e);
                }
            }
            
            log.info("═".repeat(80));
            
            if (messageDTO.getParsedData() != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> parsedData = (Map<String, Object>) messageDTO.getParsedData();
                    
                    log.info("🔄 Начало обработки распарсенных данных от Ollama для сообщения: {}", messageDTO.getMessageId());
                    log.info("🔄 Параметры: chatName={}, senderName={}, senderPhone={}", 
                            messageDTO.getChatName(), messageDTO.getSenderName(), messageDTO.getSenderPhoneNumber());
                    log.info("🔄 Детальная информация об отправителе:");
                    log.info("   senderId: {}", messageDTO.getSenderId());
                    log.info("   senderName: {}", messageDTO.getSenderName());
                    log.info("   senderPhoneNumber: {}", messageDTO.getSenderPhoneNumber());
                    
                    // ВАЛИДАЦИЯ: Проверяем, что senderPhoneNumber не является WhatsApp ID
                    String sellerPhone = messageDTO.getSenderPhoneNumber();
                    String sellerName = messageDTO.getSenderName();
                    
                    // Если senderPhoneNumber похож на WhatsApp ID (длинная числовая строка > 15 символов или содержит @)
                    if (sellerPhone != null && (sellerPhone.length() > 15 || sellerPhone.contains("@") || sellerPhone.contains("_"))) {
                        log.warn("⚠️  senderPhoneNumber похож на WhatsApp ID, а не на номер телефона: {}", sellerPhone);
                        // Пытаемся извлечь номер из senderId
                        if (messageDTO.getSenderId() != null) {
                            String extractedPhone = messageDTO.getSenderId().replaceAll("@.*", "").trim();
                            if (extractedPhone.length() <= 15 && !extractedPhone.contains("_") && extractedPhone.matches("^[0-9]+$")) {
                                sellerPhone = extractedPhone;
                                log.info("✅ Извлечен номер телефона из senderId: {}", sellerPhone);
                            } else {
                                log.warn("⚠️  Не удалось извлечь номер из senderId: {}", messageDTO.getSenderId());
                                sellerPhone = null; // Не используем WhatsApp ID как номер телефона
                            }
                        } else {
                            sellerPhone = null;
                        }
                    }
                    
                    // Если senderName похож на WhatsApp ID (длинная числовая строка > 15 символов)
                    if (sellerName != null && sellerName.length() > 15 && sellerName.matches("^[0-9]+$")) {
                        log.warn("⚠️  senderName похож на WhatsApp ID, а не на имя: {}", sellerName);
                        sellerName = null; // Не используем WhatsApp ID как имя
                    }
                    
                    log.info("🔄 Исправленные параметры: sellerName={}, sellerPhone={}", sellerName, sellerPhone);
                    
                    isUpdate = productService.processParsedData(
                            parsedData,
                            messageDTO.getMessageId(),
                            messageDTO.getChatName(),
                            sellerName,
                            sellerPhone,
                            null // location будет извлечена из parsedData
                    );
                    
                    log.info("═".repeat(100));
                    log.info("✅ ОБРАБОТКА РАСПАРСЕННЫХ ДАННЫХ ЗАВЕРШЕНА");
                    log.info("═".repeat(100));
                    log.info("   Результат: isUpdate={}", isUpdate);
                    log.info("═".repeat(100));
                    
                    // Если обнаружено обновление, но originalMessageId еще не установлен - устанавливаем
                    if (isUpdate && saved.getOriginalMessageId() == null && originalMessageId == null) {
                        originalMessageId = messageService.findPreviousMessageIdFromSeller(
                                messageDTO.getSenderPhoneNumber(), 
                                messageDTO.getChatId(),
                                messageDTO.getMessageId()
                        );
                        if (originalMessageId != null) {
                            saved.setIsUpdate(true);
                            saved.setOriginalMessageId(originalMessageId);
                            messageService.updateMessage(saved);
                        }
                    }
                } catch (Exception e) {
                    log.error("❌ ОШИБКА при обработке распарсенных данных!", e);
                    log.error("❌ Сообщение ошибки: {}", e.getMessage());
                    log.error("❌ Стек ошибки:", e);
                    // Не прерываем выполнение, если ошибка в обработке товаров
                }
            } else {
                log.info("═".repeat(100));
                log.warn("⚠️  PARSED DATA == NULL, ОБРАБОТКА ТОВАРОВ НЕ ВЫПОЛНЕНА");
                log.info("═".repeat(100));
            }
            
            String responseMessage = isUpdate 
                    ? "Сообщение успешно сохранено и обновлены существующие предложения" 
                    : "Сообщение успешно сохранено";
            
            log.info("═".repeat(100));
            log.info("✅✅✅ ОБРАБОТКА WEBHOOK ЗАВЕРШЕНА УСПЕШНО ✅✅✅");
            log.info("═".repeat(100));
            log.info("   messageId: {}", messageDTO.getMessageId());
            log.info("   saved.getId(): {}", saved.getId());
            log.info("   isUpdate: {}", isUpdate);
            log.info("   responseMessage: {}", responseMessage);
            log.info("═".repeat(100));
            
            return ResponseEntity.ok()
                    .body(new WebhookResponse(true, responseMessage, saved.getId()));
        } catch (Exception e) {
            log.error("Ошибка при сохранении сообщения", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new WebhookResponse(false, "Ошибка при сохранении: " + e.getMessage(), null));
        }
    }
    
    /**
     * Endpoint для приема сообщений от Telegram сервиса
     */
    @PostMapping(value = "/telegram", produces = "application/json;charset=UTF-8", 
                 consumes = "application/json;charset=UTF-8")
    public ResponseEntity<?> receiveTelegramMessage(
            @Valid @RequestBody WhatsAppMessageDTO messageDTO,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        
        // Логируем входящие данные для диагностики кодировки
        log.info("Получен webhook от Telegram сервиса: messageId={}", messageDTO.getMessageId());
        if (messageDTO.getChatName() != null) {
            byte[] chatNameBytes = messageDTO.getChatName().getBytes(StandardCharsets.UTF_8);
            log.debug("chatName (UTF-8 bytes): {}", java.util.Arrays.toString(chatNameBytes));
            log.info("chatName (as received): '{}'", messageDTO.getChatName());
            log.info("chatName (length): {}", messageDTO.getChatName().length());
            log.info("chatName (contains Cyrillic): {}", messageDTO.getChatName().matches(".*[А-Яа-яЁё].*"));
        }
        if (messageDTO.getContent() != null) {
            String preview = messageDTO.getContent().substring(0, Math.min(100, messageDTO.getContent().length()));
            log.debug("content preview (first 100): {}", preview);
            log.debug("content (contains Cyrillic): {}", messageDTO.getContent().matches(".*[А-Яа-яЁё].*"));
        }
        
        try {
            // Проверяем, есть ли предыдущие сообщения от этого продавца (для определения обновлений)
            String originalMessageId = null;
            // Для Telegram используем senderId вместо senderPhoneNumber
            String senderIdentifier = messageDTO.getSenderPhoneNumber() != null 
                    ? messageDTO.getSenderPhoneNumber() 
                    : messageDTO.getSenderId();
            
            if (senderIdentifier != null && messageDTO.getParsedData() != null) {
                originalMessageId = messageService.findPreviousMessageIdFromSeller(
                        senderIdentifier, 
                        messageDTO.getChatId(),
                        messageDTO.getMessageId()
                );
            }
            
            // Сохраняем сообщение с информацией об обновлении
            WhatsAppMessage saved = messageService.saveMessage(messageDTO, originalMessageId);
            
            // Если это сообщение из группы, синхронизируем информацию о группе
            if ("group".equals(messageDTO.getChatType()) && messageDTO.getChatId() != null) {
                try {
                    groupService.syncGroupsFromMessages();
                } catch (Exception e) {
                    log.warn("Ошибка при синхронизации групп: {}", e.getMessage());
                }
            }
            
            // Обрабатываем распарсенные данные от Ollama, если они есть
            boolean isUpdate = false;
            log.info("═".repeat(80));
            log.info("ПРОВЕРКА PARSED DATA ДЛЯ СООБЩЕНИЯ TELEGRAM: {}", messageDTO.getMessageId());
            log.info("═".repeat(80));
            
            if (messageDTO.getParsedData() == null) {
                log.warn("⚠️  parsedData == null - распарсенные данные от Ollama отсутствуют!");
            } else {
                log.info("✅ parsedData присутствует, тип: {}", messageDTO.getParsedData().getClass().getName());
                
                // Логируем содержимое parsedData
                try {
                    com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    String parsedDataJson = objectMapper.writeValueAsString(messageDTO.getParsedData());
                    log.info("📋 Содержимое parsedData (JSON):");
                    log.info(parsedDataJson);
                    
                    // Проверяем структуру
                    if (messageDTO.getParsedData() instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> parsedDataMap = (Map<String, Object>) messageDTO.getParsedData();
                        log.info("📋 parsedData является Map, ключи: {}", parsedDataMap.keySet());
                        log.info("📋 operationType: {}", parsedDataMap.get("operationType"));
                        log.info("📋 location: {}", parsedDataMap.get("location"));
                        
                        Object productsObj = parsedDataMap.get("products");
                        if (productsObj instanceof java.util.List) {
                            @SuppressWarnings("unchecked")
                            java.util.List<Map<String, Object>> products = (java.util.List<Map<String, Object>>) productsObj;
                            log.info("📋 products - список, размер: {}", products.size());
                        } else {
                            log.warn("⚠️  products не является List, тип: {}", productsObj != null ? productsObj.getClass().getName() : "null");
                        }
                    } else {
                        log.warn("⚠️  parsedData не является Map, тип: {}", messageDTO.getParsedData().getClass().getName());
                    }
                } catch (Exception e) {
                    log.error("❌ Ошибка при логировании parsedData: {}", e.getMessage(), e);
                }
            }
            
            log.info("═".repeat(80));
            
            if (messageDTO.getParsedData() != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> parsedData = (Map<String, Object>) messageDTO.getParsedData();
                    
                    log.info("🔄 Начало обработки распарсенных данных от Ollama для сообщения Telegram: {}", messageDTO.getMessageId());
                    log.info("🔄 Параметры: chatName={}, senderName={}, senderId={}", 
                            messageDTO.getChatName(), messageDTO.getSenderName(), senderIdentifier);
                    
                    isUpdate = productService.processParsedData(
                            parsedData,
                            messageDTO.getMessageId(),
                            messageDTO.getChatName(),
                            messageDTO.getSenderName(),
                            senderIdentifier,
                            null // location будет извлечена из parsedData
                    );
                    
                    log.info("✅ Обработка распарсенных данных завершена. isUpdate={}", isUpdate);
                    
                    // Если обнаружено обновление, но originalMessageId еще не установлен - устанавливаем
                    if (isUpdate && saved.getOriginalMessageId() == null && originalMessageId == null) {
                        originalMessageId = messageService.findPreviousMessageIdFromSeller(
                                senderIdentifier, 
                                messageDTO.getChatId(),
                                messageDTO.getMessageId()
                        );
                        if (originalMessageId != null) {
                            saved.setIsUpdate(true);
                            saved.setOriginalMessageId(originalMessageId);
                            messageService.updateMessage(saved);
                        }
                    }
                } catch (Exception e) {
                    log.error("❌ ОШИБКА при обработке распарсенных данных!", e);
                    log.error("❌ Сообщение ошибки: {}", e.getMessage());
                    log.error("❌ Стек ошибки:", e);
                    // Не прерываем выполнение, если ошибка в обработке товаров
                }
            }
            
            String responseMessage = isUpdate 
                    ? "Сообщение успешно сохранено и обновлены существующие предложения" 
                    : "Сообщение успешно сохранено";
            
            return ResponseEntity.ok()
                    .body(new WebhookResponse(true, responseMessage, saved.getId()));
        } catch (Exception e) {
            log.error("Ошибка при сохранении сообщения Telegram", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new WebhookResponse(false, "Ошибка при сохранении: " + e.getMessage(), null));
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        long messageCount = messageService.getTotalMessages();
        return ResponseEntity.ok().body(new WebhookResponse(true, 
                "API работает. Сообщений в БД: " + messageCount, null));
    }
    
    /**
     * Endpoint для проверки сообщений (для диагностики)
     */
    @GetMapping("/messages/count")
    public ResponseEntity<?> getMessagesCount() {
        long total = messageService.getTotalMessages();
        long groups = messageService.getMessagesCountByType("group");
        long personal = messageService.getMessagesCountByType("personal");
        
        var response = new java.util.HashMap<String, Object>();
        response.put("total", total);
        response.put("groups", groups);
        response.put("personal", personal);
        
        return ResponseEntity.ok().body(response);
    }
    
    // Внутренний класс для ответа
    private record WebhookResponse(boolean success, String message, Long messageId) {}
}
