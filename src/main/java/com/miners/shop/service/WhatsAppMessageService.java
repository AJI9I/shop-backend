package com.miners.shop.service;

import com.miners.shop.dto.WhatsAppMessageDTO;
import com.miners.shop.entity.WhatsAppMessage;
import com.miners.shop.repository.WhatsAppMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppMessageService {
    
    private final WhatsAppMessageRepository messageRepository;
    
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    
    /**
     * Находит ID предыдущего сообщения от того же продавца в том же чате
     * Используется для определения обновлений
     */
    @Transactional(readOnly = true)
    public String findPreviousMessageIdFromSeller(String sellerPhone, String chatId, String currentMessageId) {
        if (sellerPhone == null || sellerPhone.isEmpty() || chatId == null || chatId.isEmpty()) {
            return null;
        }
        
        // Ищем последние сообщения от этого продавца в этом чате
        List<WhatsAppMessage> previousMessages = messageRepository.findBySenderPhoneNumberAndChatIdOrderByTimestampDesc(
                sellerPhone, chatId);
        
        // Пропускаем текущее сообщение, если оно уже есть в БД
        Optional<WhatsAppMessage> previous = previousMessages.stream()
                .filter(m -> !m.getMessageId().equals(currentMessageId))
                .findFirst();
        
        if (previous.isPresent()) {
            log.debug("Найдено предыдущее сообщение от продавца {}: {}", sellerPhone, previous.get().getMessageId());
            return previous.get().getMessageId();
        }
        
        return null;
    }
    
    /**
     * Сохраняет или обновляет сообщение из WhatsApp
     * @param dto - данные сообщения
     * @param originalMessageId - ID оригинального сообщения, если это обновление (может быть null)
     */
    @Transactional
    public WhatsAppMessage saveMessage(WhatsAppMessageDTO dto, String originalMessageId) {
        log.info("Получено сообщение: messageId={}, chatName={}, senderName={}", 
                dto.getMessageId(), dto.getChatName(), dto.getSenderName());
        
        // Проверяем, существует ли уже сообщение с таким messageId
        Optional<WhatsAppMessage> existing = messageRepository.findByMessageId(dto.getMessageId());
        
        WhatsAppMessage message;
        if (existing.isPresent()) {
            message = existing.get();
            log.debug("Обновление существующего сообщения: id={}", message.getId());
        } else {
            message = new WhatsAppMessage();
            message.setMessageId(dto.getMessageId());
            log.debug("Создание нового сообщения");
        }
        
        // Устанавливаем информацию об обновлении
        if (originalMessageId != null && !originalMessageId.isEmpty()) {
            message.setIsUpdate(true);
            message.setOriginalMessageId(originalMessageId);
            log.info("Сообщение помечено как обновление оригинального сообщения: {}", originalMessageId);
        }
        
        // Обновляем поля (явно обрабатываем UTF-8)
        message.setChatId(dto.getChatId() != null ? dto.getChatId() : "");
        message.setChatName(fixEncoding(dto.getChatName()));
        message.setChatType(dto.getChatType() != null ? dto.getChatType() : "group");
        message.setSenderId(dto.getSenderId() != null ? dto.getSenderId() : "");
        message.setSenderName(dto.getSenderName() != null ? fixEncoding(dto.getSenderName()) : "Unknown");
        message.setSenderPhoneNumber(dto.getSenderPhoneNumber());
        message.setContent(dto.getContent() != null ? fixEncoding(dto.getContent()) : "");
        
        // Парсим timestamp
        if (dto.getTimestamp() != null && !dto.getTimestamp().isEmpty()) {
            try {
                message.setTimestamp(LocalDateTime.parse(dto.getTimestamp(), ISO_FORMATTER));
            } catch (Exception e) {
                log.warn("Ошибка парсинга timestamp: {}, используем текущее время", dto.getTimestamp());
                message.setTimestamp(LocalDateTime.now());
            }
        } else {
            message.setTimestamp(LocalDateTime.now());
        }
        
        message.setHasMedia(dto.getHasMedia() != null ? dto.getHasMedia() : false);
        message.setMediaMimetype(dto.getMediaMimetype());
        message.setMediaFilename(dto.getMediaFilename());
        message.setMediaData(dto.getMediaData());
        message.setMessageType(dto.getMessageType());
        message.setIsForwarded(dto.getIsForwarded() != null ? dto.getIsForwarded() : false);
        
        // Сохраняем обработанные данные от нейросети (parsedData) как JSON строку
        if (dto.getParsedData() != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String parsedDataJson = objectMapper.writeValueAsString(dto.getParsedData());
                message.setParsedData(parsedDataJson);
                log.debug("Сохранены обработанные данные от нейросети для сообщения: messageId={}", dto.getMessageId());
            } catch (Exception e) {
                log.error("Ошибка при сохранении parsedData: {}", e.getMessage(), e);
                message.setParsedData(null);
            }
        } else {
            message.setParsedData(null);
        }
        
        WhatsAppMessage saved = messageRepository.save(message);
        log.info("Сообщение сохранено: id={}, messageId={}, isUpdate={}", 
                saved.getId(), saved.getMessageId(), saved.getIsUpdate());
        
        return saved;
    }
    
    /**
     * Обновляет существующее сообщение (для установки флага обновления после обработки товаров)
     */
    @Transactional
    public void updateMessage(WhatsAppMessage message) {
        messageRepository.save(message);
        log.debug("Сообщение обновлено: id={}, messageId={}, isUpdate={}", 
                message.getId(), message.getMessageId(), message.getIsUpdate());
    }
    
    /**
     * Получает все сообщения с пагинацией
     */
    @Transactional(readOnly = true)
    public Page<WhatsAppMessage> getAllMessages(Pageable pageable) {
        return messageRepository.findAllByOrderByTimestampDesc(pageable);
    }
    
    /**
     * Получает сообщения по типу чата с пагинацией
     */
    @Transactional(readOnly = true)
    public Page<WhatsAppMessage> getMessagesByChatType(String chatType, Pageable pageable) {
        return messageRepository.findByChatTypeOrderByTimestampDesc(chatType, pageable);
    }
    
    /**
     * Получает последние сообщения
     */
    @Transactional(readOnly = true)
    public List<WhatsAppMessage> getRecentMessages(int limit) {
        Pageable pageable = Pageable.ofSize(limit);
        return messageRepository.findAllByOrderByTimestampDesc(pageable).getContent();
    }
    
    /**
     * Получает сообщения по chatId
     */
    @Transactional(readOnly = true)
    public List<WhatsAppMessage> getMessagesByChatId(String chatId) {
        return messageRepository.findByChatIdOrderByTimestampDesc(chatId);
    }
    
    /**
     * Получает сообщение по ID
     */
    @Transactional(readOnly = true)
    public Optional<WhatsAppMessage> getMessageById(Long id) {
        return messageRepository.findById(id);
    }
    
    /**
     * Получает сообщение по messageId
     */
    @Transactional(readOnly = true)
    public Optional<WhatsAppMessage> getMessageByMessageId(String messageId) {
        return messageRepository.findByMessageId(messageId);
    }
    
    /**
     * Преобразует parsedData из JSON строки в объект
     */
    public Object parseParsedData(String parsedDataJson) {
        if (parsedDataJson == null || parsedDataJson.trim().isEmpty()) {
            return null;
        }
        
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(parsedDataJson, Object.class);
        } catch (Exception e) {
            log.error("Ошибка при парсинге parsedData: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Удаляет сообщение
     */
    @Transactional
    public void deleteMessage(Long id) {
        messageRepository.deleteById(id);
        log.info("Сообщение удалено: id={}", id);
    }
    
    /**
     * Получает статистику
     */
    @Transactional(readOnly = true)
    public long getTotalMessages() {
        return messageRepository.count();
    }
    
    @Transactional(readOnly = true)
    public long getMessagesCountByType(String chatType) {
        return messageRepository.countByChatType(chatType);
    }
    
    /**
     * Исправляет кодировку текста
     * Пытается определить и исправить проблемы с кодировкой UTF-8
     */
    private String fixEncoding(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // Если текст уже содержит правильную кириллицу, возвращаем как есть
        if (text.matches(".*[А-Яа-яЁё].*")) {
            return text;
        }
        
        // Проверяем, нет ли искаженных символов (признак проблемы с кодировкой)
        // Искаженные символы обычно выглядят как последовательности типа "Рў", "Рµ", "СЃ" и т.д.
        boolean hasDistortedChars = text.contains("Р") || text.contains("С") || text.contains("Рў") || 
                                   text.contains("Рµ") || text.contains("СЃ") || text.contains("Рѕ");
        
        if (hasDistortedChars) {
            try {
                // Попытка 1: Если текст был интерпретирован как ISO-8859-1, но на самом деле это UTF-8
                // Преобразуем через ISO-8859-1 и обратно в UTF-8
                byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
                String decoded = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                
                // Проверяем, содержит ли результат кириллицу
                if (decoded.matches(".*[А-Яа-яЁё].*") && !text.matches(".*[А-Яа-яЁё].*")) {
                    log.info("Исправлена кодировка (ISO-8859-1->UTF-8): {} -> {}", 
                             text.substring(0, Math.min(50, text.length())), 
                             decoded.substring(0, Math.min(50, decoded.length())));
                    return decoded;
                }
            } catch (Exception e) {
                log.debug("Попытка 1 не удалась: {}", e.getMessage());
            }
            
            try {
                // Попытка 2: Если текст был закодирован дважды в UTF-8
                // Декодируем один раз через Windows-1251 (часто используется для русских символов)
                byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
                String decoded = new String(bytes, java.nio.charset.Charset.forName("Windows-1251"));
                
                if (decoded.matches(".*[А-Яа-яЁё].*") && !text.matches(".*[А-Яа-яЁё].*")) {
                    log.info("Исправлена кодировка (Windows-1251): {} -> {}", 
                             text.substring(0, Math.min(50, text.length())), 
                             decoded.substring(0, Math.min(50, decoded.length())));
                    return decoded;
                }
            } catch (Exception e) {
                log.debug("Попытка 2 не удалась: {}", e.getMessage());
            }
            
            try {
                // Попытка 3: Прямое преобразование через UTF-8 байты
                byte[] utf8Bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                String test = new String(utf8Bytes, java.nio.charset.StandardCharsets.UTF_8);
                if (test.matches(".*[А-Яа-яЁё].*") && !text.matches(".*[А-Яа-яЁё].*")) {
                    log.info("Исправлена кодировка (прямое UTF-8): {} -> {}", 
                             text.substring(0, Math.min(50, text.length())), 
                             test.substring(0, Math.min(50, test.length())));
                    return test;
                }
            } catch (Exception e) {
                log.debug("Попытка 3 не удалась: {}", e.getMessage());
            }
        }
        
        // Если ничего не помогло, возвращаем исходный текст
        log.warn("Не удалось исправить кодировку для текста: {}", 
                 text.substring(0, Math.min(100, text.length())));
        return text;
    }
}
