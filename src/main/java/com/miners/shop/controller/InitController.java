package com.miners.shop.controller;

import com.miners.shop.repository.ProductRepository;
import com.miners.shop.service.MinerDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Контроллер для инициализации данных (временный, для миграции)
 * ВНИМАНИЕ: Этот контроллер следует удалить или защитить после завершения инициализации
 */
@RestController
@RequestMapping("/api/init")
@RequiredArgsConstructor
@Slf4j
public class InitController {
    
    private final MinerDetailService minerDetailService;
    private final ProductRepository productRepository;
    
    /**
     * Экспорт данных из таблицы products для анализа
     * GET /api/init/export-products
     * 
     * Возвращает все товары из таблицы products в формате JSON
     */
    @GetMapping("/export-products")
    public ResponseEntity<Map<String, Object>> exportProducts() {
        try {
            log.info("Запрос на экспорт данных из таблицы products");
            
            List<com.miners.shop.entity.Product> products = productRepository.findAll();
            
            List<Map<String, Object>> productsData = products.stream()
                    .map(product -> {
                        Map<String, Object> data = new HashMap<>();
                        data.put("id", product.getId());
                        data.put("model", product.getModel());
                        data.put("manufacturer", product.getManufacturer());
                        data.put("description", product.getDescription());
                        data.put("createdAt", product.getCreatedAt() != null ? product.getCreatedAt().toString() : null);
                        data.put("updatedAt", product.getUpdatedAt() != null ? product.getUpdatedAt().toString() : null);
                        data.put("minerDetailId", product.getMinerDetail() != null ? product.getMinerDetail().getId() : null);
                        return data;
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalCount", products.size());
            response.put("products", productsData);
            
            log.info("Экспорт завершен: {} товаров", products.size());
            
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(response);
        } catch (Exception e) {
            log.error("Ошибка при экспорте товаров: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Ошибка при экспорте: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Инициализация MinerDetail для существующих Product
     * POST /api/init/miner-details
     * 
     * Создает MinerDetail для всех товаров, у которых еще нет детальной записи
     */
    @PostMapping("/miner-details")
    public ResponseEntity<Map<String, Object>> initializeMinerDetails() {
        try {
            log.info("Запрос на инициализацию MinerDetail для существующих товаров");
            
            int createdCount = minerDetailService.initializeMinerDetailsForExistingProducts();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Инициализация завершена успешно");
            response.put("createdCount", createdCount);
            
            log.info("Инициализация завершена: создано {} записей MinerDetail", createdCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка при инициализации MinerDetail: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Ошибка при инициализации: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Полная инициализация MinerDetail с учетом групп объединения
     * POST /api/init/miner-details-with-groups
     * 
     * Шаги:
     * 1. Исправляет ошибки в manufacturer для товаров
     * 2. Создает MinerDetail для групп объединения (12 групп)
     * 3. Создает MinerDetail для уникальных майнеров (~35 товаров)
     * 4. Связывает все Product с соответствующими MinerDetail
     */
    @PostMapping("/miner-details-with-groups")
    public ResponseEntity<Map<String, Object>> initializeMinersDetailsWithGroups() {
        try {
            log.info("Запрос на полную инициализацию MinerDetail с группами");
            
            int createdCount = minerDetailService.initializeMinersDetailsWithGroups();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Полная инициализация завершена успешно");
            response.put("createdCount", createdCount);
            
            log.info("Полная инициализация завершена: создано {} записей MinerDetail", createdCount);
            
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(response);
        } catch (Exception e) {
            log.error("Ошибка при полной инициализации MinerDetail: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Ошибка при инициализации: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}

