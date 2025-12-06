package com.miners.shop.controller;

import com.miners.shop.dto.CompanyMinerDTO;
import com.miners.shop.dto.MinerDetailDTO;
import com.miners.shop.entity.CompanyMiner;
import com.miners.shop.entity.MinerDetail;
import com.miners.shop.entity.Product;
import com.miners.shop.repository.CompanyMinerRepository;
import com.miners.shop.repository.ProductRepository;
import com.miners.shop.service.CompanyMinerService;
import com.miners.shop.service.ImageUploadService;
import com.miners.shop.service.MinerDetailExcelService;
import com.miners.shop.service.MinerDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Контроллер для управления детальной информацией о майнерах
 */
@Controller
@RequestMapping("/private/miner-details")
@RequiredArgsConstructor
@Slf4j
public class MinerDetailController {
    
    private final MinerDetailService minerDetailService;
    private final ProductRepository productRepository;
    private final MinerDetailExcelService excelService;
    private final ImageUploadService imageUploadService;
    private final CompanyMinerRepository companyMinerRepository;
    private final CompanyMinerService companyMinerService;
    
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
        
        // Добавляем количество товаров для каждого MinerDetail
        Map<Long, Integer> productCounts = new HashMap<>();
        for (MinerDetail detail : minerDetails) {
            int count = detail.getProducts() != null ? detail.getProducts().size() : 0;
            productCounts.put(detail.getId(), count);
        }
        
        model.addAttribute("minerDetails", dtos);
        model.addAttribute("productCounts", productCounts);
        model.addAttribute("manufacturers", minerDetailService.getDistinctManufacturers());
        model.addAttribute("series", minerDetailService.getDistinctSeries());
        model.addAttribute("algorithms", minerDetailService.getDistinctAlgorithms());
        
        // Добавляем пустой объект для формы создания
        if (!model.containsAttribute("minerDetail")) {
            model.addAttribute("minerDetail", new MinerDetailDTO());
        }
        
        return "miner-details/list";
    }
    
    /**
     * Страница создания новой детальной записи
     * Должен быть объявлен ПЕРЕД /{id}, чтобы не перехватывался общим маршрутом
     */
    @GetMapping("/new")
    public String createForm(Model model) {
        MinerDetailDTO dto = new MinerDetailDTO();
        model.addAttribute("minerDetail", dto);
        model.addAttribute("allProducts", productRepository.findAll());
        return "miner-details/create";
    }
    
    /**
     * REST API: Экспорт детальных записей в Excel
     * Поддерживает фильтрацию по производителям, сериям и моделям
     * Должен быть объявлен ПЕРЕД /{id}, чтобы не перехватывался общим маршрутом
     * 
     * @param manufacturers Список производителей для фильтрации (опционально)
     * @param series Список серий для фильтрации (опционально)
     * @param standardNames Список названий моделей для фильтрации (опционально)
     */
    @GetMapping("/export")
    @ResponseBody
    public ResponseEntity<?> exportToExcel(
            @RequestParam(required = false) List<String> manufacturers,
            @RequestParam(required = false) List<String> series,
            @RequestParam(required = false) List<String> standardNames) {
        try {
            log.info("Запрос на экспорт MinerDetail в Excel. Фильтры: manufacturers={}, series={}, standardNames={}", 
                    manufacturers, series, standardNames);
            
            // Получаем все детальные записи
            List<MinerDetail> allMinerDetails = minerDetailService.getAllMinerDetails();
            
            // Применяем фильтры
            List<MinerDetail> filteredDetails = allMinerDetails.stream()
                    .filter(detail -> {
                        // Фильтр по производителям
                        if (manufacturers != null && !manufacturers.isEmpty()) {
                            if (detail.getManufacturer() == null || 
                                !manufacturers.contains(detail.getManufacturer())) {
                                return false;
                            }
                        }
                        
                        // Фильтр по сериям
                        if (series != null && !series.isEmpty()) {
                            if (detail.getSeries() == null || 
                                !series.contains(detail.getSeries())) {
                                return false;
                            }
                        }
                        
                        // Фильтр по названиям моделей
                        if (standardNames != null && !standardNames.isEmpty()) {
                            if (detail.getStandardName() == null || 
                                !standardNames.contains(detail.getStandardName())) {
                                return false;
                            }
                        }
                        
                        return true;
                    })
                    .collect(Collectors.toList());
            
            log.info("Отфильтровано {} записей из {} для экспорта", 
                    filteredDetails.size(), allMinerDetails.size());
            
            // Экспортируем в Excel
            ByteArrayOutputStream outputStream = excelService.exportToExcel(filteredDetails);
            
            // Формируем имя файла
            String filename = "miner-details-export-" + 
                    java.time.LocalDate.now().toString() + ".xlsx";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(outputStream.toByteArray());
                    
        } catch (Exception e) {
            log.error("Ошибка при экспорте MinerDetail в Excel: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Ошибка при экспорте: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * REST API: Импорт детальных записей из Excel
     * Принимает Excel файл и обновляет существующие записи по ID
     * Должен быть объявлен ПЕРЕД /{id}, чтобы не перехватывался общим маршрутом
     */
    @PostMapping("/import")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> importFromExcel(
            @RequestParam("file") MultipartFile file) {
        try {
            log.info("Запрос на импорт MinerDetail из Excel файла: {}", file.getOriginalFilename());
            
            if (file.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Файл пуст");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Проверяем расширение файла
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Файл должен быть в формате Excel (.xlsx или .xls)");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Импортируем из Excel
            List<MinerDetailDTO> dtos = excelService.importFromExcel(file.getInputStream());
            
            if (dtos == null || dtos.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Не удалось прочитать данные из файла или файл пуст");
                return ResponseEntity.badRequest().body(error);
            }
            
            log.info("Прочитано {} записей из Excel файла", dtos.size());
            
            int updatedCount = 0;
            int errorCount = 0;
            List<String> errors = new java.util.ArrayList<>();
            
            // Обрабатываем каждую запись
            for (MinerDetailDTO dto : dtos) {
                try {
                    // Проверяем, что ID указан
                    if (dto.getId() == null) {
                        errors.add("Запись без ID пропущена: " + dto.getStandardName());
                        errorCount++;
                        continue;
                    }
                    
                    // Проверяем, существует ли запись с таким ID
                    Optional<MinerDetail> existingOpt = minerDetailService.getMinerDetailById(dto.getId());
                    if (existingOpt.isEmpty()) {
                        errors.add("Запись с ID=" + dto.getId() + " не найдена, пропущена");
                        errorCount++;
                        continue;
                    }
                    
                    // Обновляем запись
                    MinerDetail existing = existingOpt.get();
                    
                    // Обновляем поля из DTO, но сохраняем важные данные
                    existing.setStandardName(dto.getStandardName());
                    existing.setManufacturer(dto.getManufacturer());
                    existing.setSeries(dto.getSeries());
                    existing.setHashrate(dto.getHashrate());
                    existing.setAlgorithm(dto.getAlgorithm());
                    existing.setPowerConsumption(dto.getPowerConsumption());
                    existing.setCoins(dto.getCoins());
                    existing.setPowerSource(dto.getPowerSource());
                    existing.setCooling(dto.getCooling());
                    existing.setOperatingTemperature(dto.getOperatingTemperature());
                    existing.setDimensions(dto.getDimensions());
                    existing.setNoiseLevel(dto.getNoiseLevel());
                    existing.setDescription(dto.getDescription());
                    existing.setFeatures(dto.getFeatures());
                    existing.setPlacementInfo(dto.getPlacementInfo());
                    existing.setProducerInfo(dto.getProducerInfo());
                    // createdAt и products не трогаем - они остаются как есть
                    // updatedAt будет установлен автоматически через @PreUpdate
                    
                    minerDetailService.updateMinerDetail(existing);
                    updatedCount++;
                    
                    log.debug("Обновлена запись MinerDetail ID={}, название={}", 
                            dto.getId(), dto.getStandardName());
                    
                } catch (Exception e) {
                    errorCount++;
                    String errorMsg = "Ошибка при обновлении записи ID=" + dto.getId() + ": " + e.getMessage();
                    errors.add(errorMsg);
                    log.error(errorMsg, e);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Импорт завершен");
            response.put("total", dtos.size());
            response.put("updated", updatedCount);
            response.put("errors", errorCount);
            response.put("errorDetails", errors);
            
            log.info("Импорт завершен: обновлено {}, ошибок {}", updatedCount, errorCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Ошибка при импорте MinerDetail: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Ошибка при импорте: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * REST API: Создание новой детальной записи
     * Должен быть объявлен ПЕРЕД /{id}, чтобы не перехватывался общим маршрутом
     */
    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createJson(@RequestBody MinerDetailDTO dto) {
        try {
            log.info("Запрос на создание нового MinerDetail: {}", dto.getStandardName());
            
            if (dto.getStandardName() == null || dto.getStandardName().trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Стандартизированное название обязательно");
                return ResponseEntity.badRequest().body(error);
            }
            
            MinerDetail minerDetail = dto.toEntity();
            MinerDetail saved = minerDetailService.updateMinerDetail(minerDetail);
            
            log.info("Создан новый MinerDetail: ID={}, название={}", saved.getId(), saved.getStandardName());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "MinerDetail успешно создан");
            response.put("data", MinerDetailDTO.fromEntity(saved));
            
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(response);
                    
        } catch (Exception e) {
            log.error("Ошибка при создании MinerDetail: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Ошибка при создании: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * REST API: Получить детальную запись в JSON
     * Должен быть объявлен ПЕРЕД /{id}, чтобы не перехватывался общим маршрутом
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
     * REST API: Просмотр и анализ данных MinerDetail с указанием отсутствующих полей
     * Должен быть объявлен ПЕРЕД /{id}, чтобы не перехватывался общим маршрутом
     */
    @GetMapping("/api/{id}/analyze")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> analyzeMinerDetail(@PathVariable Long id) {
        try {
            Optional<MinerDetail> minerDetailOpt = minerDetailService.getMinerDetailById(id);
            if (minerDetailOpt.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "MinerDetail с ID=" + id + " не найден");
                return ResponseEntity.notFound().build();
            }
            
            MinerDetail minerDetail = minerDetailOpt.get();
            MinerDetailDTO dto = MinerDetailDTO.fromEntity(minerDetail);
            
            // Определяем отсутствующие поля
            List<String> missingFields = new java.util.ArrayList<>();
            if (minerDetail.getHashrate() == null || minerDetail.getHashrate().trim().isEmpty()) {
                missingFields.add("hashrate");
            }
            if (minerDetail.getAlgorithm() == null || minerDetail.getAlgorithm().trim().isEmpty()) {
                missingFields.add("algorithm");
            }
            if (minerDetail.getPowerConsumption() == null || minerDetail.getPowerConsumption().trim().isEmpty()) {
                missingFields.add("powerConsumption");
            }
            if (minerDetail.getCooling() == null || minerDetail.getCooling().trim().isEmpty()) {
                missingFields.add("cooling");
            }
            if (minerDetail.getOperatingTemperature() == null || minerDetail.getOperatingTemperature().trim().isEmpty()) {
                missingFields.add("operatingTemperature");
            }
            if (minerDetail.getDimensions() == null || minerDetail.getDimensions().trim().isEmpty()) {
                missingFields.add("dimensions");
            }
            if (minerDetail.getNoiseLevel() == null || minerDetail.getNoiseLevel().trim().isEmpty()) {
                missingFields.add("noiseLevel");
            }
            if (minerDetail.getCoins() == null || minerDetail.getCoins().trim().isEmpty()) {
                missingFields.add("coins");
            }
            if (minerDetail.getPowerSource() == null || minerDetail.getPowerSource().trim().isEmpty()) {
                missingFields.add("powerSource");
            }
            if (minerDetail.getDescription() == null || minerDetail.getDescription().trim().isEmpty()) {
                missingFields.add("description");
            }
            if (minerDetail.getFeatures() == null || minerDetail.getFeatures().trim().isEmpty()) {
                missingFields.add("features");
            }
            if (minerDetail.getPlacementInfo() == null || minerDetail.getPlacementInfo().trim().isEmpty()) {
                missingFields.add("placementInfo");
            }
            if (minerDetail.getProducerInfo() == null || minerDetail.getProducerInfo().trim().isEmpty()) {
                missingFields.add("producerInfo");
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", dto);
            response.put("missingFields", missingFields);
            response.put("missingCount", missingFields.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка при анализе MinerDetail ID={}: {}", id, e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Ошибка при анализе: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * REST API: Обновить детальную запись
     * Должен быть объявлен ПЕРЕД /{id}, чтобы не перехватывался общим маршрутом
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
     * Должен быть объявлен ПЕРЕД /{id}, чтобы не перехватывался общим маршрутом
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
    
    /**
     * Страница детального просмотра одного майнера (со всей информацией)
     * Отображает все данные MinerDetail: технические характеристики, описания, связанные товары
     * Должен быть объявлен ПОСЛЕ всех специфичных маршрутов
     */
    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        MinerDetail minerDetail = minerDetailService.getMinerDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("MinerDetail не найден: " + id));
        
        MinerDetailDTO dto = MinerDetailDTO.fromEntity(minerDetail);
        
        // Получаем список связанных товаров (Product)
        List<Product> linkedProducts = minerDetail.getProducts();
        
        // Проверяем наличие CompanyMiner для этого MinerDetail
        Optional<CompanyMiner> companyMinerOpt = companyMinerRepository.findByMinerDetailId(id);
        boolean hasCompanyMiner = companyMinerOpt.isPresent();
        CompanyMinerDTO.CompanyMinerInfo companyMinerInfo = null;
        Long companyMinerId = null;
        
        if (hasCompanyMiner) {
            companyMinerInfo = companyMinerService.getCompanyMinerByMinerDetailId(id)
                    .orElse(null);
            if (companyMinerInfo != null) {
                companyMinerId = companyMinerInfo.id();
            }
        }
        
        model.addAttribute("minerDetail", dto);
        model.addAttribute("linkedProducts", linkedProducts);
        model.addAttribute("linkedProductsCount", linkedProducts != null ? linkedProducts.size() : 0);
        model.addAttribute("hasCompanyMiner", hasCompanyMiner);
        model.addAttribute("companyMinerId", companyMinerId);
        
        return "miner-details/view";
    }
    
    /**
     * Страница редактирования детальной записи (для администратора)
     * Должен быть объявлен ПОСЛЕ /{id}, так как содержит /{id}/edit
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
     * Создание новой детальной записи
     */
    @PostMapping
    public String create(
            @ModelAttribute MinerDetailDTO dto,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
        MinerDetail minerDetail = dto.toEntity();
        MinerDetail saved = minerDetailService.updateMinerDetail(minerDetail);
        
        // Обрабатываем загрузку изображения после создания записи (когда уже есть ID)
        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = imageUploadService.saveImage(imageFile, saved.getId());
            if (imageUrl != null) {
                saved.setImageUrl(imageUrl);
                minerDetailService.updateMinerDetail(saved);
                log.info("Изображение загружено для нового MinerDetail ID={}: {}", saved.getId(), imageUrl);
            }
        }
        
        log.info("Создана новая запись MinerDetail: ID={}, название={}", saved.getId(), saved.getStandardName());
            return "redirect:/private/miner-details/" + saved.getId() + "?success=true";
    }
    
    /**
     * Обновление детальной записи
     */
    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id, 
            @ModelAttribute MinerDetailDTO dto,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
        
        // Получаем существующую запись для сохранения createdAt и других данных
        MinerDetail existingMinerDetail = minerDetailService.getMinerDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("MinerDetail не найден: " + id));
        
        MinerDetail minerDetail = dto.toEntity();
        minerDetail.setId(id);
        minerDetail.setCreatedAt(existingMinerDetail.getCreatedAt()); // Сохраняем дату создания
        
        // Обрабатываем загрузку изображения
        if (imageFile != null && !imageFile.isEmpty()) {
            // Если загружен новый файл, сохраняем его
            String imageUrl = imageUploadService.saveImage(imageFile, id);
            if (imageUrl != null) {
                // Удаляем старое изображение, если оно было и отличается от нового
                if (existingMinerDetail.getImageUrl() != null && !existingMinerDetail.getImageUrl().isEmpty() 
                        && !existingMinerDetail.getImageUrl().equals(imageUrl)) {
                    imageUploadService.deleteImage(existingMinerDetail.getImageUrl());
                }
                minerDetail.setImageUrl(imageUrl);
                log.info("Изображение загружено для MinerDetail ID={}: {}", id, imageUrl);
            } else {
                // Если загрузка не удалась, используем значение из DTO или старое
                if (dto.getImageUrl() != null && !dto.getImageUrl().isEmpty()) {
                    minerDetail.setImageUrl(dto.getImageUrl());
                } else {
                    minerDetail.setImageUrl(existingMinerDetail.getImageUrl());
                }
                log.warn("Не удалось загрузить изображение для MinerDetail ID={}, используем значение из формы или старое", id);
            }
        } else {
            // Если файл не загружен, используем значение из DTO (может быть указан URL вручную)
            if (dto.getImageUrl() != null) {
                minerDetail.setImageUrl(dto.getImageUrl());
            } else {
                // Если в DTO тоже нет, сохраняем старое изображение
                minerDetail.setImageUrl(existingMinerDetail.getImageUrl());
            }
        }
        
        minerDetailService.updateMinerDetail(minerDetail);
            return "redirect:/private/miner-details/" + id + "?success=true";
    }
    
    /**
     * REST API: Переключение активности MinerDetail
     * Должен быть объявлен ПЕРЕД /{id}, чтобы не перехватывался общим маршрутом
     */
    @PostMapping("/api/{id}/toggle-active")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleActive(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {
        try {
            log.info("Запрос на переключение активности MinerDetail ID={}", id);
            
            Optional<MinerDetail> minerDetailOpt = minerDetailService.getMinerDetailById(id);
            if (minerDetailOpt.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "MinerDetail с ID=" + id + " не найден");
                return ResponseEntity.notFound().build();
            }
            
            MinerDetail minerDetail = minerDetailOpt.get();
            Boolean newActive = request.get("active");
            
            if (newActive == null) {
                // Если значение не передано, переключаем на противоположное
                newActive = minerDetail.getActive() == null || !minerDetail.getActive();
            }
            
            minerDetail.setActive(newActive);
            minerDetailService.updateMinerDetail(minerDetail);
            
            log.info("Активность MinerDetail ID={} изменена на: {}", id, newActive);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Активность успешно обновлена");
            response.put("id", id);
            response.put("active", newActive);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка при переключении активности MinerDetail ID={}: {}", id, e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Ошибка при обновлении активности: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
}
