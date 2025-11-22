package com.miners.shop.controller;

import com.miners.shop.config.AssetsCopyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Контроллер для управления ресурсами шаблона
 */
@RestController
@RequestMapping("/api/admin/assets")
@Slf4j
public class AssetsController {
    
    /**
     * Принудительно копирует ресурсы шаблона
     */
    @PostMapping("/copy")
    public ResponseEntity<Map<String, Object>> copyAssets() {
        log.info("🔄 Запрос на принудительное копирование ресурсов");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            AssetsCopyUtil.copyAssetsIfNeeded();
            response.put("success", true);
            response.put("message", "Ресурсы скопированы (или уже были скопированы)");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка при копировании ресурсов: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Ошибка: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}

