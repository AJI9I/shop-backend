package com.miners.shop.controller;

import com.miners.shop.dto.TelegramGroupDTO;
import com.miners.shop.service.TelegramGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/telegram/groups")
@RequiredArgsConstructor
@Slf4j
public class TelegramGroupController {
    
    private final TelegramGroupService groupService;
    
    /**
     * Получает список всех доступных групп Telegram
     */
    @GetMapping
    public ResponseEntity<List<TelegramGroupDTO>> getAllGroups() {
        log.info("Запрос списка всех групп Telegram");
        List<TelegramGroupDTO> groups = groupService.getAllGroups();
        return ResponseEntity.ok(groups);
    }
    
    /**
     * Получает список групп с включенным мониторингом
     */
    @GetMapping("/active")
    public ResponseEntity<List<TelegramGroupDTO>> getActiveGroups() {
        log.info("Запрос списка активных групп Telegram");
        List<TelegramGroupDTO> groups = groupService.getActiveGroups();
        return ResponseEntity.ok(groups);
    }
    
    /**
     * Включает мониторинг для группы
     */
    @PostMapping("/{chatId}/enable")
    public ResponseEntity<TelegramGroupDTO> enableMonitoring(@PathVariable String chatId) {
        log.info("Включение мониторинга для группы: {}", chatId);
        TelegramGroupDTO group = groupService.toggleMonitoring(chatId, true);
        return ResponseEntity.ok(group);
    }
    
    /**
     * Выключает мониторинг для группы
     */
    @PostMapping("/{chatId}/disable")
    public ResponseEntity<TelegramGroupDTO> disableMonitoring(@PathVariable String chatId) {
        log.info("Выключение мониторинга для группы: {}", chatId);
        TelegramGroupDTO group = groupService.toggleMonitoring(chatId, false);
        return ResponseEntity.ok(group);
    }
    
    /**
     * Переключает статус мониторинга для группы
     */
    @PostMapping("/{chatId}/toggle")
    public ResponseEntity<Map<String, Object>> toggleMonitoring(@PathVariable String chatId) {
        log.info("Переключение мониторинга для группы: {}", chatId);
        
        // Получаем текущий статус
        List<TelegramGroupDTO> allGroups = groupService.getAllGroups();
        TelegramGroupDTO currentGroup = allGroups.stream()
                .filter(g -> g.getChatId().equals(chatId))
                .findFirst()
                .orElse(null);
        
        if (currentGroup == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Группа не найдена");
            return ResponseEntity.notFound().build();
        }
        
        // Переключаем статус
        boolean newStatus = !currentGroup.getMonitoringEnabled();
        TelegramGroupDTO updatedGroup = groupService.toggleMonitoring(chatId, newStatus);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("group", updatedGroup);
        response.put("monitoringEnabled", updatedGroup.getMonitoringEnabled());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Синхронизирует список групп из сообщений
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncGroups() {
        log.info("Синхронизация групп из сообщений");
        groupService.syncGroupsFromMessages();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Группы успешно синхронизированы");
        
        return ResponseEntity.ok(response);
    }
}


