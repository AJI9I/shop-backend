package com.miners.shop.controller;

import com.miners.shop.dto.MinerDetailDTO;
import com.miners.shop.entity.MinerDetail;
import com.miners.shop.entity.Product;
import com.miners.shop.repository.ProductRepository;
import com.miners.shop.service.MinerDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Контроллер для управления детальной информацией о майнерах
 */
@Controller
@RequestMapping("/miner-details")
@RequiredArgsConstructor
@Slf4j
public class MinerDetailController {
    
    private final MinerDetailService minerDetailService;
    private final ProductRepository productRepository;
    
    /**
     * Страница со списком всех детальных записей (таблица с основными параметрами)
     * Отображает таблицу с 4-5 основными параметрами для быстрого просмотра
     * 
     * Основные параметры в таблице:
     * 1. Стандартизированное название (standardName)
     * 2. Производитель (manufacturer)
     * 3. Хэшрейт (hashrate)
     * 4. Алгоритм (algorithm)
     * 5. Потребление (powerConsumption)
     */
    @GetMapping
    public String list(Model model) {
        List<MinerDetail> minerDetails = minerDetailService.getAllMinerDetails();
        List<MinerDetailDTO> dtos = minerDetails.stream()
                .map(MinerDetailDTO::fromEntity)
                .collect(Collectors.toList());
        
        model.addAttribute("minerDetails", dtos);
        model.addAttribute("manufacturers", minerDetailService.getDistinctManufacturers());
        model.addAttribute("series", minerDetailService.getDistinctSeries());
        model.addAttribute("algorithms", minerDetailService.getDistinctAlgorithms());
        
        return "miner-details/list";
    }
    
    /**
     * Страница детального просмотра одного майнера (со всей информацией)
     * Отображает все данные MinerDetail: технические характеристики, описания, связанные товары
     */
    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        MinerDetail minerDetail = minerDetailService.getMinerDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("MinerDetail не найден: " + id));
        
        MinerDetailDTO dto = MinerDetailDTO.fromEntity(minerDetail);
        
        // Получаем список связанных товаров (Product)
        List<Product> linkedProducts = minerDetail.getProducts();
        
        model.addAttribute("minerDetail", dto);
        model.addAttribute("linkedProducts", linkedProducts);
        model.addAttribute("linkedProductsCount", linkedProducts != null ? linkedProducts.size() : 0);
        
        return "miner-details/view";
    }
    
    /**
     * Страница редактирования детальной записи (для администратора)
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        MinerDetail minerDetail = minerDetailService.getMinerDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("MinerDetail не найден: " + id));
        
        MinerDetailDTO dto = MinerDetailDTO.fromEntity(minerDetail);
        
        // Получаем список всех товаров для возможности объединения
        List<Product> allProducts = productRepository.findAll();
        
        model.addAttribute("minerDetail", dto);
        model.addAttribute("allProducts", allProducts);
        model.addAttribute("linkedProducts", minerDetail.getProducts());
        
        return "miner-details/edit";
    }
    
    /**
     * Обновление детальной записи
     */
    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute MinerDetailDTO dto) {
        MinerDetail minerDetail = dto.toEntity();
        minerDetail.setId(id);
        minerDetailService.updateMinerDetail(minerDetail);
        return "redirect:/miner-details/" + id + "?success=true";
    }
    
    /**
     * REST API: Получить детальную запись в JSON
     */
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<MinerDetailDTO> getJson(@PathVariable Long id) {
        return minerDetailService.getMinerDetailById(id)
                .map(MinerDetailDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * REST API: Обновить детальную запись
     */
    @PutMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateJson(@PathVariable Long id, @RequestBody MinerDetailDTO dto) {
        try {
            MinerDetail minerDetail = dto.toEntity();
            minerDetail.setId(id);
            MinerDetail updated = minerDetailService.updateMinerDetail(minerDetail);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Детальная запись успешно обновлена");
            response.put("data", MinerDetailDTO.fromEntity(updated));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка при обновлении детальной записи: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Ошибка при обновлении: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * REST API: Объединить товары
     * 
     * Объединяет выбранные товары в целевую детальную запись
     * 
     * Пример запроса:
     * POST /miner-details/api/1/merge
     * {
     *   "productIds": [2, 3, 5]
     * }
     * 
     * Это означает: объединить Product ID=2, ID=3, ID=5 в целевую MinerDetail ID=1
     * 
     * @param id ID целевой MinerDetail (в которую объединяются товары)
     * @param request Тело запроса с полем productIds - список ID товаров для объединения
     * @return Результат операции
     */
    @PostMapping("/api/{id}/merge")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> mergeProducts(@PathVariable Long id, @RequestBody Map<String, List<Long>> request) {
        try {
            // Получаем список ID товаров из запроса
            List<Long> productIds = request.get("productIds");
            if (productIds == null || productIds.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Список ID товаров (productIds) обязателен");
                return ResponseEntity.badRequest().body(error);
            }
            
            log.info("Запрос на объединение {} товаров в целевую MinerDetail ID={}", productIds.size(), id);
            
            // Выполняем объединение: все выбранные товары привязываются к целевой MinerDetail
            minerDetailService.mergeProducts(id, productIds);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Товары успешно объединены в целевую MinerDetail");
            response.put("targetMinerDetailId", id);
            response.put("mergedProductIds", productIds);
            response.put("mergedCount", productIds.size());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Ошибка валидации при объединении товаров: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Ошибка при объединении товаров: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Ошибка при объединении: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}

