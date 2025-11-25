package com.miners.shop.service;

import com.miners.shop.dto.TelegramGroupDTO;
import com.miners.shop.entity.TelegramGroup;
import com.miners.shop.entity.WhatsAppMessage;
import com.miners.shop.repository.TelegramGroupRepository;
import com.miners.shop.repository.WhatsAppMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramGroupService {
    
    private final TelegramGroupRepository groupRepository;
    private final WhatsAppMessageRepository messageRepository;
    
    /**
     * Получает список всех доступных групп Telegram
     */
    @Transactional(readOnly = true)
    public List<TelegramGroupDTO> getAllGroups() {
        // Сначала синхронизируем группы из сообщений
        syncGroupsFromMessages();
        
        List<TelegramGroup> groups = groupRepository.findAllByOrderByChatNameAsc();
        return groups.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Получает список групп с включенным мониторингом
     */
    @Transactional(readOnly = true)
    public List<TelegramGroupDTO> getActiveGroups() {
        syncGroupsFromMessages();
        
        List<TelegramGroup> groups = groupRepository.findActiveGroups();
        return groups.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Включает или выключает мониторинг для группы
     */
    @Transactional
    public TelegramGroupDTO toggleMonitoring(String chatId, Boolean enabled) {
        Optional<TelegramGroup> groupOpt = groupRepository.findByChatId(chatId);
        
        TelegramGroup group;
        if (groupOpt.isPresent()) {
            group = groupOpt.get();
        } else {
            // Если группы нет, создаем новую
            group = new TelegramGroup();
            group.setChatId(chatId);
            
            // Получаем название группы из последнего сообщения
            List<WhatsAppMessage> messages = messageRepository.findByChatIdOrderByTimestampDesc(chatId);
            if (!messages.isEmpty()) {
                group.setChatName(messages.get(0).getChatName());
            } else {
                group.setChatName("Неизвестная группа");
            }
        }
        
        group.setMonitoringEnabled(enabled);
        group = groupRepository.save(group);
        
        log.info("Мониторинг для группы {} {}: {}", chatId, enabled ? "включен" : "выключен", group.getChatName());
        
        return toDTO(group);
    }
    
    /**
     * Синхронизирует список групп из сообщений в базе данных
     */
    @Transactional
    public void syncGroupsFromMessages() {
        log.debug("Синхронизация групп из сообщений...");
        
        // Получаем все уникальные группы из сообщений (только group, не personal)
        List<WhatsAppMessage> groupMessages = messageRepository.findByChatTypeOrderByTimestampDesc("group");
        
        // Группируем по chatId
        java.util.Map<String, WhatsAppMessage> latestMessagesByChat = groupMessages.stream()
                .collect(java.util.stream.Collectors.toMap(
                        WhatsAppMessage::getChatId,
                        msg -> msg,
                        (existing, replacement) -> existing.getTimestamp().isAfter(replacement.getTimestamp()) 
                                ? existing : replacement
                ));
        
        // Обновляем или создаем записи в таблице telegram_groups
        for (java.util.Map.Entry<String, WhatsAppMessage> entry : latestMessagesByChat.entrySet()) {
            String chatId = entry.getKey();
            WhatsAppMessage latestMessage = entry.getValue();
            
            Optional<TelegramGroup> groupOpt = groupRepository.findByChatId(chatId);
            
            TelegramGroup group;
            if (groupOpt.isPresent()) {
                group = groupOpt.get();
                // Обновляем название, если оно изменилось
                if (!group.getChatName().equals(latestMessage.getChatName())) {
                    group.setChatName(latestMessage.getChatName());
                }
            } else {
                // Создаем новую группу
                group = new TelegramGroup();
                group.setChatId(chatId);
                group.setChatName(latestMessage.getChatName());
                group.setMonitoringEnabled(true); // По умолчанию включаем мониторинг
            }
            
            // Обновляем статистику
            long messageCount = messageRepository.findByChatIdOrderByTimestampDesc(chatId).size();
            group.setMessageCount(messageCount);
            
            // Обновляем дату последнего сообщения
            if (latestMessage.getTimestamp() != null) {
                group.setLastMessageDate(latestMessage.getTimestamp());
            }
            
            groupRepository.save(group);
        }
        
        log.debug("Синхронизация завершена. Обработано групп: {}", latestMessagesByChat.size());
    }
    
    /**
     * Преобразует Entity в DTO
     */
    private TelegramGroupDTO toDTO(TelegramGroup group) {
        TelegramGroupDTO dto = new TelegramGroupDTO();
        dto.setId(group.getId());
        dto.setChatId(group.getChatId());
        dto.setChatName(group.getChatName());
        dto.setMonitoringEnabled(group.getMonitoringEnabled());
        dto.setMessageCount(group.getMessageCount());
        dto.setLastMessageDate(group.getLastMessageDate());
        dto.setCreatedAt(group.getCreatedAt());
        dto.setUpdatedAt(group.getUpdatedAt());
        return dto;
    }
}


