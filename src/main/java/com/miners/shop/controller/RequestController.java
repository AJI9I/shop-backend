package com.miners.shop.controller;

import com.miners.shop.dto.RequestDTO;
import com.miners.shop.entity.Request;
import com.miners.shop.entity.Request.RequestStatus;
import com.miners.shop.service.RequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Контроллер для работы с заявками
 */
@Controller
@RequestMapping("/private/requests")
@RequiredArgsConstructor
@Slf4j
public class RequestController {
    
    private final RequestService requestService;
    
    /**
     * REST API: Создать новую заявку
     */
    @PostMapping("/api/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createRequest(
            @RequestBody RequestDTO.CreateRequestDTO createDTO) {
        try {
            log.info("Получен запрос на создание заявки для предложения ID={}", createDTO.offerId());
            
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
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Ошибка при создании заявки: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Неожиданная ошибка при создании заявки", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Внутренняя ошибка сервера");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Страница со списком заявок
     */
    @GetMapping
    public String requestsList(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            Model model) {
        
        // Настройка пагинации и сортировки
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Получаем заявки
        Page<Request> requestsPage;
        if (status != null && !status.isEmpty()) {
            try {
                RequestStatus requestStatus = RequestStatus.valueOf(status.toUpperCase());
                requestsPage = requestService.getRequestsByStatus(requestStatus, pageable);
            } catch (IllegalArgumentException e) {
                requestsPage = requestService.getAllRequests(pageable);
            }
        } else {
            requestsPage = requestService.getAllRequests(pageable);
        }
        
        // Получаем статистику
        Map<String, Long> statistics = requestService.getRequestStatistics();
        
        model.addAttribute("requests", requestsPage);
        model.addAttribute("statistics", statistics);
        model.addAttribute("currentStatus", status != null ? status : "");
        model.addAttribute("currentPage", page);
        model.addAttribute("currentSize", size);
        
        return "requests";
    }
    
    /**
     * Страница просмотра детальной информации о заявке
     * При открытии заявки автоматически меняется статус на "просмотрено" (PROCESSED)
     */
    @GetMapping("/{id}")
    public String requestDetails(@PathVariable Long id, Model model) {
        try {
            // Помечаем заявку как просмотренную (меняем статус с NEW на PROCESSED)
            requestService.markAsViewed(id);
            
            // Получаем заявку с полной информацией
            Request request = requestService.getRequestById(id);
            model.addAttribute("request", request);
            model.addAttribute("requestDTO", RequestDTO.fromEntity(request));
            return "request-details";
        } catch (IllegalArgumentException e) {
            log.error("Заявка с ID={} не найдена", id);
            return "redirect:/private/requests?error=not_found";
        }
    }
    
    /**
     * REST API: Получить заявку в JSON формате
     */
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<RequestDTO> getRequestJson(@PathVariable Long id) {
        try {
            Request request = requestService.getRequestById(id);
            return ResponseEntity.ok(RequestDTO.fromEntity(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}


