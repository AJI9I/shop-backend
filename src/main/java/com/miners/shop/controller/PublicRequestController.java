package com.miners.shop.controller;

import com.miners.shop.dto.RequestDTO;
import com.miners.shop.entity.Request;
import com.miners.shop.service.RequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Публичный контроллер для создания заявок (без авторизации)
 * Используется для создания заявок со страниц товаров
 */
@RestController
@RequestMapping("/requests/api")
@RequiredArgsConstructor
@Slf4j
public class PublicRequestController {
    
    private final RequestService requestService;
    
    /**
     * REST API: Создать новую заявку (публичный доступ)
     */
    @PostMapping(value = "/create", produces = "application/json;charset=UTF-8")
    public ResponseEntity<Map<String, Object>> createRequest(
            @RequestBody RequestDTO.CreateRequestDTO createDTO) {
        try {
            log.info("Получен публичный запрос на создание заявки для предложения ID={}", createDTO.offerId());
            
            // Валидация
            if (createDTO.offerId() == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "ID предложения обязателен");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (createDTO.clientName() == null || createDTO.clientName().trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Имя клиента обязательно");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (createDTO.clientPhone() == null || createDTO.clientPhone().trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Телефон клиента обязателен");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Создаем заявку
            Request request = requestService.createRequest(createDTO);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Заявка успешно создана");
            response.put("requestId", request.getId());
            
            log.info("Заявка успешно создана: ID={}", request.getId());
            
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(response);
        } catch (IllegalArgumentException e) {
            log.error("Ошибка при создании заявки: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest()
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(error);
        } catch (Exception e) {
            log.error("Неожиданная ошибка при создании заявки", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Внутренняя ошибка сервера");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(error);
        }
    }
}

