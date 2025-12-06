package com.miners.shop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miners.shop.dto.OfferDTO;
import com.miners.shop.dto.CompanyMinerDTO;
import com.miners.shop.entity.CompanyMiner;
import com.miners.shop.entity.MinerDetail;
import com.miners.shop.entity.OperationType;
import com.miners.shop.entity.Offer;
import com.miners.shop.entity.Product;
import com.miners.shop.repository.CompanyMinerRepository;
import com.miners.shop.service.CompanyMinerService;
import com.miners.shop.service.ProductService;
import com.miners.shop.util.SchemaOrgUtil;
import com.miners.shop.util.SeoUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Контроллер для страницы товаров
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ProductsController {
    
    private final ProductService productService;
    private final com.miners.shop.repository.ProductRepository productRepository;
    private final com.miners.shop.repository.OfferRepository offerRepository;
    private final com.miners.shop.repository.MinerDetailRepository minerDetailRepository;
    private final com.miners.shop.service.MinerDetailService minerDetailService;
    private final com.miners.shop.util.ImageUrlResolver imageUrlResolver;
    private final ObjectMapper objectMapper;
    private final CompanyMinerRepository companyMinerRepository;
    private final CompanyMinerService companyMinerService;
    
    /**
     * Страница с таблицей всех продуктов
     * Отображает все товары в виде таблицы с подробной информацией
     */
    @GetMapping("/private/products/table")
    @Transactional(readOnly = true)
    public String productsTable(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false, defaultValue = "updatedAt") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDir,
            @RequestParam(required = false) String manufacturer,
            Model model) {
        try {
            // Создаем Pageable с сортировкой
            Sort.Direction direction = sortDir.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Sort sort = Sort.by(direction, sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // Получаем список уникальных производителей для фильтров
            List<String> manufacturers = productRepository.findDistinctManufacturers();
            
            // Получаем товары с фильтрацией по производителю
            Page<Product> productsPage;
            if (manufacturer != null && !manufacturer.isEmpty()) {
                productsPage = productRepository.findByManufacturer(manufacturer, pageable);
            } else {
                productsPage = productRepository.findAll(pageable);
            }
            
            // Загружаем ID продуктов для батч-загрузки offers
            List<Long> productIds = productsPage.getContent().stream()
                    .map(Product::getId)
                    .toList();
            
            // Загружаем все offers для этих продуктов одним запросом
            if (!productIds.isEmpty()) {
                List<Offer> allOffers = offerRepository.findByProductIdIn(productIds);
                // Группируем offers по productId
                Map<Long, List<Offer>> offersByProductId = allOffers.stream()
                        .collect(java.util.stream.Collectors.groupingBy(o -> o.getProduct().getId()));
                
                // Назначаем offers каждому продукту
                productsPage.getContent().forEach(product -> {
                    List<Offer> productOffers = offersByProductId.getOrDefault(product.getId(), new java.util.ArrayList<>());
                    // Инициализируем продавцов
                    productOffers.forEach(offer -> {
                        if (offer.getSeller() != null) {
                            offer.getSeller().getName();
                        }
                    });
                    product.getOffers().clear();
                    product.getOffers().addAll(productOffers);
                });
            }
            
            // Загружаем MinerDetail для всех продуктов
            List<Long> minerDetailIds = productsPage.getContent().stream()
                    .map(Product::getMinerDetail)
                    .filter(java.util.Objects::nonNull)
                    .map(MinerDetail::getId)
                    .distinct()
                    .toList();
            
            final Map<Long, MinerDetail> minerDetailsMap;
            if (!minerDetailIds.isEmpty()) {
                List<MinerDetail> minerDetails = minerDetailRepository.findAllById(minerDetailIds);
                minerDetailsMap = minerDetails.stream()
                        .collect(java.util.stream.Collectors.toMap(MinerDetail::getId, md -> md));
            } else {
                minerDetailsMap = new HashMap<>();
            }
            
            // Вычисляем статистику для каждого продукта
            productsPage.getContent().forEach(product -> {
                try {
                    // Инициализируем коллекцию offers
                    if (product.getOffers() != null) {
                        // Находим минимальную цену для продажи
                        java.util.Optional<java.math.BigDecimal> minPrice = product.getOffers().stream()
                                .filter(o -> o.getOperationType() != null && o.getOperationType() == OperationType.SELL)
                                .filter(o -> o.getPrice() != null)
                                .map(offer -> offer.getPrice())
                                .min(java.util.Comparator.naturalOrder());
                        
                        product.setMinPrice(minPrice.orElse(null));
                    }
                } catch (Exception e) {
                    log.warn("Ошибка при обработке продукта ID={}: {}", product.getId(), e.getMessage());
                    product.setMinPrice(null);
                }
                
                // Инициализируем MinerDetail (если есть)
                if (product.getMinerDetail() != null && product.getMinerDetail().getId() != null) {
                    MinerDetail loadedMinerDetail = minerDetailsMap.get(product.getMinerDetail().getId());
                    if (loadedMinerDetail != null) {
                        // Заменяем proxy на реальный объект
                        product.setMinerDetail(loadedMinerDetail);
                    }
                }
                
                // Устанавливаем URL изображения
                String imageUrl = imageUrlResolver.resolveImageUrl(product.getModel());
                product.setImageUrl(imageUrl);
            });
            
            // Получаем все MinerDetail для выбора при редактировании
            List<MinerDetail> allMinerDetails = minerDetailService.getAllMinerDetails();
            
            model.addAttribute("productsPage", productsPage);
            model.addAttribute("currentPage", page);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDir", sortDir);
            model.addAttribute("manufacturers", manufacturers);
            model.addAttribute("currentManufacturer", manufacturer != null ? manufacturer : "");
            model.addAttribute("allMinerDetails", allMinerDetails);
            
            return "products-table";
        } catch (Exception e) {
            log.error("Ошибка при загрузке таблицы продуктов", e);
            model.addAttribute("error", "Ошибка при загрузке данных: " + e.getMessage());
            return "products-table";
        }
    }
    
    /**
     * Главная страница товаров (майнеров с предложениями)
     * Показывает только товары, у которых есть предложения
     * По умолчанию сортирует по последнему обновлению предложений (самые свежие первыми)
     */
    @GetMapping("/products")
    @Transactional(readOnly = true)
    public String products(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,  // Увеличено до 12 для карточек
            @RequestParam(required = false, defaultValue = "latestOffer") String sortBy, // latestOffer, name, manufacturer
            @RequestParam(required = false) List<String> manufacturer, // Фильтр по производителям
            @RequestParam(required = false) List<String> series, // Фильтр по сериям
            @RequestParam(required = false) String search, // Поиск по названию, производителю или серии
            HttpServletRequest request,
            Model model) {
        try {
            Page<MinerDetail> minerDetailsPage = new org.springframework.data.domain.PageImpl<>(List.of());
            Pageable pageable = PageRequest.of(page, size);
            
            // Выбираем метод сортировки в зависимости от параметра
            // ВАЖНО: Все типы сортировки должны учитывать фильтры по manufacturer и series
            
            // Получаем все MinerDetail с фильтрами (если есть)
            List<MinerDetail> filteredMinerDetails;
            
            // Если есть поисковый запрос, сначала фильтруем по нему
            if (search != null && !search.trim().isEmpty()) {
                filteredMinerDetails = minerDetailRepository.findAllWithOffersBySearch(search.trim());
                
                // Применяем дополнительные фильтры по производителю (если есть)
                if (manufacturer != null && !manufacturer.isEmpty()) {
                    filteredMinerDetails = filteredMinerDetails.stream()
                            .filter(md -> md.getManufacturer() != null && manufacturer.contains(md.getManufacturer()))
                            .collect(java.util.stream.Collectors.toList());
                }
                
                // Применяем дополнительные фильтры по серии (если есть)
                if (series != null && !series.isEmpty()) {
                    filteredMinerDetails = filteredMinerDetails.stream()
                            .filter(md -> md.getSeries() != null && series.contains(md.getSeries()))
                            .collect(java.util.stream.Collectors.toList());
                }
            } else if ((manufacturer == null || manufacturer.isEmpty()) && (series == null || series.isEmpty())) {
                // Без фильтров - получаем все
                filteredMinerDetails = minerDetailRepository.findAllWithOffers();
            } else {
                // С фильтрами - используем методы фильтрации
                if (manufacturer != null && !manufacturer.isEmpty() && series != null && !series.isEmpty()) {
                    // Оба фильтра
                    filteredMinerDetails = minerDetailRepository.findAllWithOffersByManufacturersAndSeries(manufacturer, series);
                } else if (manufacturer != null && !manufacturer.isEmpty()) {
                    // Только производитель
                    filteredMinerDetails = minerDetailRepository.findAllWithOffersByManufacturers(manufacturer);
                } else {
                    // Только серия
                    filteredMinerDetails = minerDetailRepository.findAllWithOffersBySeries(series);
                }
            }
            
            // Применяем сортировку в зависимости от параметра
            switch (sortBy) {
                case "name":
                    // Сортировка по алфавиту (по названию)
                    filteredMinerDetails.sort((md1, md2) -> {
                        String name1 = md1.getStandardName() != null ? md1.getStandardName() : "";
                        String name2 = md2.getStandardName() != null ? md2.getStandardName() : "";
                        return name1.compareToIgnoreCase(name2);
                    });
                    break;
                case "manufacturer":
                    // Сортировка по производителю, затем по названию
                    filteredMinerDetails.sort((md1, md2) -> {
                        String man1 = md1.getManufacturer() != null ? md1.getManufacturer() : "";
                        String man2 = md2.getManufacturer() != null ? md2.getManufacturer() : "";
                        int cmp = man1.compareToIgnoreCase(man2);
                        if (cmp == 0) {
                            String name1 = md1.getStandardName() != null ? md1.getStandardName() : "";
                            String name2 = md2.getStandardName() != null ? md2.getStandardName() : "";
                            return name1.compareToIgnoreCase(name2);
                        }
                        return cmp;
                    });
                    break;
                case "latestOffer":
                default:
                    // Сортировка по последнему обновлению предложений (по умолчанию)
                    // Если есть поиск или другие фильтры, используем filteredMinerDetails и сортируем вручную
                    if (search != null && !search.trim().isEmpty() || 
                        (manufacturer != null && !manufacturer.isEmpty()) || 
                        (series != null && !series.isEmpty())) {
                        // Используем метод сервиса для сортировки по последнему обновлению предложений
                        // Но сначала фильтруем результаты
                        filteredMinerDetails = minerDetailService.sortByLatestOfferUpdate(filteredMinerDetails);
                        // Применяем пагинацию вручную
                        int start = (int) pageable.getOffset();
                        int end = Math.min(start + pageable.getPageSize(), filteredMinerDetails.size());
                        
                        if (start >= filteredMinerDetails.size()) {
                            minerDetailsPage = new org.springframework.data.domain.PageImpl<>(List.of(), pageable, filteredMinerDetails.size());
                        } else {
                            if (start < 0) start = 0;
                            if (end <= start) end = Math.min(start + pageable.getPageSize(), filteredMinerDetails.size());
                            if (end > filteredMinerDetails.size()) end = filteredMinerDetails.size();
                            
                            try {
                                if (start >= 0 && start < filteredMinerDetails.size() && end > start && end <= filteredMinerDetails.size()) {
                                    List<MinerDetail> pageContent = filteredMinerDetails.subList(start, end);
                                    minerDetailsPage = new org.springframework.data.domain.PageImpl<>(pageContent, pageable, filteredMinerDetails.size());
                                } else {
                                    minerDetailsPage = new org.springframework.data.domain.PageImpl<>(filteredMinerDetails, pageable, filteredMinerDetails.size());
                                }
                            } catch (IndexOutOfBoundsException e) {
                                log.error("Ошибка IndexOutOfBoundsException при сортировке по latestOffer: start={}, end={}, size={}", 
                                        start, end, filteredMinerDetails.size(), e);
                                minerDetailsPage = new org.springframework.data.domain.PageImpl<>(filteredMinerDetails, pageable, filteredMinerDetails.size());
                            }
                        }
                    } else {
                        // Без фильтров - используем метод сервиса напрямую
                        minerDetailsPage = minerDetailService.findAllSortedByLatestOfferUpdate(pageable, manufacturer, series);
                    }
                    // Переходим к обработке результатов
                    filteredMinerDetails = new java.util.ArrayList<>(); // Пустой список для избежания null
                    break;
            }
            
            // Для сортировок "name" и "manufacturer" применяем пагинацию вручную
            if (!"latestOffer".equals(sortBy)) {
                if (filteredMinerDetails != null && !filteredMinerDetails.isEmpty()) {
                    // Применяем пагинацию
                    int start = (int) pageable.getOffset();
                    int end = Math.min(start + pageable.getPageSize(), filteredMinerDetails.size());
                    
                    // Проверяем, что start не больше размера списка
                    if (start >= filteredMinerDetails.size()) {
                        minerDetailsPage = new org.springframework.data.domain.PageImpl<>(List.of(), pageable, filteredMinerDetails.size());
                    } else {
                        // Убеждаемся, что start не отрицательный
                        if (start < 0) {
                            start = 0;
                        }
                        // Убеждаемся, что end не меньше start и не больше размера списка
                        if (end <= start) {
                            end = Math.min(start + pageable.getPageSize(), filteredMinerDetails.size());
                        }
                        if (end > filteredMinerDetails.size()) {
                            end = filteredMinerDetails.size();
                        }
                        
                        // Дополнительная защита от IndexOutOfBoundsException при малом количестве товаров
                        try {
                            if (start >= 0 && start < filteredMinerDetails.size() && end > start && end <= filteredMinerDetails.size()) {
                                List<MinerDetail> pageContent = filteredMinerDetails.subList(start, end);
                                minerDetailsPage = new org.springframework.data.domain.PageImpl<>(pageContent, pageable, filteredMinerDetails.size());
                            } else {
                                // Если границы невалидны, возвращаем все товары или пустую страницу
                                log.warn("Некорректные границы пагинации: start={}, end={}, size={}. Возвращаем все товары.", 
                                        start, end, filteredMinerDetails.size());
                                minerDetailsPage = new org.springframework.data.domain.PageImpl<>(filteredMinerDetails, pageable, filteredMinerDetails.size());
                            }
                        } catch (IndexOutOfBoundsException e) {
                            // Обработка исключения IndexOutOfBoundsException
                            log.error("Ошибка IndexOutOfBoundsException при создании подсписка: start={}, end={}, size={}", 
                                    start, end, filteredMinerDetails.size(), e);
                            // Возвращаем все товары вместо пустой страницы
                            minerDetailsPage = new org.springframework.data.domain.PageImpl<>(filteredMinerDetails, pageable, filteredMinerDetails.size());
                        }
                    }
                } else {
                    // Если filteredMinerDetails пуст или null, создаем пустую страницу
                    minerDetailsPage = new org.springframework.data.domain.PageImpl<>(List.of(), pageable, 0);
                }
            }
            
            List<MinerDetail> minerDetails = minerDetailsPage.getContent();
            
            // ОПТИМИЗАЦИЯ: Загружаем все данные одним запросом вместо N+1
            Map<Long, List<Offer>> allOffersByMinerDetailId = new HashMap<>();
            Map<Long, ProductOperationInfo> minerDetailOperationInfo = new HashMap<>();
            Map<Long, String> productSlugs = new HashMap<>();
            
            // Собираем все ID MinerDetail
            List<Long> minerDetailIds = minerDetails.stream()
                    .map(MinerDetail::getId)
                    .toList();
            
            // Загружаем все Product для всех MinerDetail одним запросом
            List<Product> allProducts = minerDetailIds.isEmpty() 
                    ? List.of() 
                    : productRepository.findByMinerDetailIdIn(minerDetailIds);
            
            // Собираем все ID Product
            List<Long> allProductIds = allProducts.stream()
                    .map(Product::getId)
                    .toList();
            
            // Загружаем все offers для всех Product одним запросом
            List<Offer> allOffers = allProductIds.isEmpty() 
                    ? List.of() 
                    : offerRepository.findByProductIdIn(allProductIds);
            
            // Инициализируем продавцов
            allOffers.forEach(offer -> {
                if (offer.getSeller() != null) {
                    offer.getSeller().getName();
                }
            });
            
            // Группируем offers по productId, затем по minerDetailId
            Map<Long, List<Offer>> offersByProductId = allOffers.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            o -> o.getProduct() != null ? o.getProduct().getId() : null,
                            java.util.stream.Collectors.toList()
                    ));
            
            // Группируем offers по minerDetailId
            for (Product product : allProducts) {
                if (product.getMinerDetail() != null) {
                    Long minerDetailId = product.getMinerDetail().getId();
                    List<Offer> productOffers = offersByProductId.getOrDefault(product.getId(), List.of());
                    allOffersByMinerDetailId.computeIfAbsent(minerDetailId, k -> new java.util.ArrayList<>())
                            .addAll(productOffers);
                }
            }
            
            // Вычисляем productSlug для каждого MinerDetail
            for (MinerDetail minerDetail : minerDetails) {
                String productSlug = (minerDetail.getSlug() != null && !minerDetail.getSlug().isEmpty()) 
                    ? minerDetail.getSlug() 
                    : String.valueOf(minerDetail.getId());
                productSlugs.put(minerDetail.getId(), productSlug);
            }
            
            // Обрабатываем каждый MinerDetail для вычисления статистики
            for (MinerDetail minerDetail : minerDetails) {
                List<Offer> allMinerDetailOffers = allOffersByMinerDetailId.getOrDefault(minerDetail.getId(), List.of());
                
                // Вычисляем статистику для MinerDetail (суммируем offers со всех связанных Product)
                if (!allMinerDetailOffers.isEmpty()) {
                    long sellCount = allMinerDetailOffers.stream()
                            .filter(o -> o.getOperationType() != null && o.getOperationType() == OperationType.SELL)
                            .count();
                    long buyCount = allMinerDetailOffers.stream()
                            .filter(o -> o.getOperationType() != null && o.getOperationType() == OperationType.BUY)
                            .count();
                    
                    int totalQuantity = allMinerDetailOffers.stream()
                            .filter(o -> o.getQuantity() != null)
                            .mapToInt(Offer::getQuantity)
                            .sum();
                    
                    // Вычисляем минимальную цену: сначала за последние 24 часа, потом за все время
                    // Исключаем цены равные 0
                    java.util.Optional<java.math.BigDecimal> minPrice = java.util.Optional.empty();
                    String currency = null;
                    
                    // Фильтруем только предложения на продажу (SELL)
                    List<Offer> sellOffers = allMinerDetailOffers.stream()
                            .filter(o -> o.getOperationType() != null && o.getOperationType() == OperationType.SELL)
                            .filter(o -> o.getPrice() != null && o.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0)
                            .collect(java.util.stream.Collectors.toList());
                    
                    if (!sellOffers.isEmpty()) {
                        // Сначала ищем предложения за последние 24 часа
                        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
                        List<Offer> recentOffers = sellOffers.stream()
                                .filter(o -> o.getUpdatedAt() != null && o.getUpdatedAt().isAfter(oneDayAgo))
                                .collect(java.util.stream.Collectors.toList());
                        
                        if (!recentOffers.isEmpty()) {
                            // Находим минимальную цену за последние 24 часа
                            minPrice = recentOffers.stream()
                                    .map(Offer::getPrice)
                                    .min(java.util.Comparator.naturalOrder());
                            
                            if (minPrice.isPresent()) {
                                final java.math.BigDecimal finalMinPrice = minPrice.get();
                                currency = recentOffers.stream()
                                        .filter(o -> o.getPrice() != null && o.getPrice().equals(finalMinPrice))
                                        .map(Offer::getCurrency)
                                        .findFirst()
                                        .orElse("RUB");
                            } else {
                                minPrice = java.util.Optional.empty();
                            }
                        } else {
                            // Если за сутки нет предложений, ищем во всех предложениях
                            minPrice = sellOffers.stream()
                                    .map(Offer::getPrice)
                                    .min(java.util.Comparator.naturalOrder());
                            
                            if (minPrice.isPresent()) {
                                final java.math.BigDecimal finalMinPrice = minPrice.get();
                                currency = sellOffers.stream()
                                        .filter(o -> o.getPrice() != null && o.getPrice().equals(finalMinPrice))
                                        .map(Offer::getCurrency)
                                        .findFirst()
                                        .orElse("RUB");
                            }
                        }
                    }
                    
                    ProductOperationInfo info = new ProductOperationInfo();
                    info.setHasSellOffers(sellCount > 0);
                    info.setHasBuyOffers(buyCount > 0);
                    info.setSellCount(sellCount);
                    info.setBuyCount(buyCount);
                    info.setTotalQuantity(totalQuantity);
                    // Сохраняем цену только если она не null и не 0
                    if (minPrice.isPresent() && minPrice.get().compareTo(java.math.BigDecimal.ZERO) > 0) {
                        info.setMinPrice(minPrice.get());
                    } else {
                        info.setMinPrice(null);
                    }
                    info.setCurrency(currency);
                    info.setManufacturer(minerDetail.getManufacturer());
                    
                    // Определяем основной тип операции
                    if (sellCount > 0 && buyCount == 0) {
                        info.setPrimaryOperationType(OperationType.SELL);
                    } else if (buyCount > 0 && sellCount == 0) {
                        info.setPrimaryOperationType(OperationType.BUY);
                    } else if (sellCount > 0 && buyCount > 0) {
                        info.setPrimaryOperationType(OperationType.SELL);
                    }
                    
                    minerDetailOperationInfo.put(minerDetail.getId(), info);
                }
            }
            
            // Статистика
            long totalMinerDetails = minerDetailRepository.count();
            long totalProducts = productService.getTotalProducts();
            long totalOffers = productService.getTotalOffers();
            
            // Получаем списки производителей и серий для фильтров
            List<String> allManufacturers = minerDetailService.getDistinctManufacturers();
            List<String> allSeries = minerDetailService.getDistinctSeries();
            
            // Получаем маппинг серий к производителям для фильтрации чекбоксов
            Map<String, java.util.Set<String>> seriesToManufacturers = new HashMap<>();
            List<Object[]> seriesManufacturerMapping = minerDetailRepository.findSeriesManufacturerMapping();
            for (Object[] row : seriesManufacturerMapping) {
                String seriesValue = (String) row[0];
                String manufacturerValue = (String) row[1];
                if (seriesValue != null && manufacturerValue != null) {
                    seriesToManufacturers.computeIfAbsent(seriesValue, k -> new java.util.HashSet<>()).add(manufacturerValue);
                }
            }
            
            // Создаем DTO для передачи в шаблон
            List<com.miners.shop.dto.MinerDetailDTO> minerDetailDTOs = minerDetails.stream()
                    .map(com.miners.shop.dto.MinerDetailDTO::fromEntity)
                    .toList();
            
            // Для каждой MinerDetail добавляем URL изображения: сначала проверяем imageUrl из DTO, если нет - используем ImageUrlResolver
            Map<Long, String> imageUrls = new HashMap<>();
            minerDetailDTOs.forEach(dto -> {
                String imageUrl = null;
                if (dto.getImageUrl() != null && !dto.getImageUrl().trim().isEmpty()) {
                    imageUrl = dto.getImageUrl();
                } else {
                    imageUrl = imageUrlResolver.resolveImageUrl(dto.getStandardName());
                }
                imageUrls.put(dto.getId(), imageUrl);
            });
            
            model.addAttribute("minerDetails", minerDetailDTOs);
            model.addAttribute("imageUrls", imageUrls);
            model.addAttribute("minerDetailsPage", minerDetailsPage);
            model.addAttribute("productSlugs", productSlugs);
            model.addAttribute("currentPage", page);
            model.addAttribute("currentSortBy", sortBy);
            model.addAttribute("search", search != null ? search : ""); // Параметр поиска для отображения в форме
            model.addAttribute("totalMinerDetails", totalMinerDetails);
            model.addAttribute("totalProducts", totalProducts);
            model.addAttribute("totalOffers", totalOffers);
            model.addAttribute("minerDetailOperationInfo", minerDetailOperationInfo);
            
            // Данные для фильтров
            model.addAttribute("allManufacturers", allManufacturers);
            model.addAttribute("allSeries", allSeries);
            model.addAttribute("seriesToManufacturers", seriesToManufacturers); // Маппинг для фильтрации серий
            model.addAttribute("selectedManufacturers", manufacturer != null ? manufacturer : java.util.Collections.emptyList());
            model.addAttribute("selectedSeries", series != null ? series : java.util.Collections.emptyList());
            
            // Преобразуем маппинг в JSON строку для использования в JavaScript
            try {
                String seriesToManufacturersJson = objectMapper.writeValueAsString(seriesToManufacturers);
                model.addAttribute("seriesToManufacturersJson", seriesToManufacturersJson);
            } catch (Exception e) {
                log.error("Ошибка при сериализации seriesToManufacturers в JSON: {}", e.getMessage(), e);
                model.addAttribute("seriesToManufacturersJson", "{}");
            }
            
            // SEO мета-теги
            String pageTitle = "Каталог товаров - MinerHive";
            String pageDescription = "Каталог ASIC майнеров для майнинга криптовалют. Bitmain, MicroBT, Canaan. Фильтры по производителю, серии, поиск.";
            String pageKeywords = "каталог майнеров, ASIC майнеры, купить майнер, Bitmain, MicroBT";
            String canonicalUrl = SeoUtil.generateCanonicalUrl(request.getRequestURI());
            String ogImage = "https://minerhive.ru/assets/images/logo/logo.png";
            
            model.addAttribute("pageTitle", pageTitle);
            model.addAttribute("pageDescription", pageDescription);
            model.addAttribute("pageKeywords", pageKeywords);
            model.addAttribute("canonicalUrl", canonicalUrl);
            model.addAttribute("ogImage", ogImage);
            
            // Schema.org для WebSite и Organization
            model.addAttribute("websiteSchema", SchemaOrgUtil.generateWebSiteSchema());
            model.addAttribute("organizationSchema", SchemaOrgUtil.generateOrganizationSchema());
            
            return "products-new";
        } catch (Exception e) {
            // Логируем ошибку для отладки
            log.error("Ошибка при загрузке страницы товаров: {}", e.getMessage(), e);
            model.addAttribute("error", "Произошла ошибка при загрузке страницы: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Страница детальной информации о майнере (MinerDetail)
     * Показывает все предложения всех связанных товаров (Product)
     * @param id ID MinerDetail (не Product!)
     */
    @GetMapping("/products/{idOrSlug}")
    @Transactional(readOnly = true)
    public String productDetails(
            @PathVariable String idOrSlug,
            @RequestParam(required = false) String dateFilter, // today, 3days, week, month
            @RequestParam(required = false, defaultValue = "updatedAt") String sortBy, // Колонка для сортировки
            @RequestParam(required = false, defaultValue = "DESC") String sortDir, // Направление сортировки
            @RequestParam(required = false, defaultValue = "0") int page, // Страница
            @RequestParam(required = false, defaultValue = "10") int size, // По умолчанию 10 записей при первой загрузке // Количество записей на странице
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        try {
            // Пытаемся определить, это ID или slug
            Optional<MinerDetail> minerDetailOpt = Optional.empty();
            
            // Сначала пытаемся найти по slug
            minerDetailOpt = minerDetailRepository.findBySlug(idOrSlug);
            
            // Если не найден по slug, пытаемся найти по ID
            if (minerDetailOpt.isEmpty()) {
                try {
                    Long id = Long.parseLong(idOrSlug);
                    minerDetailOpt = minerDetailRepository.findById(id);
                } catch (NumberFormatException e) {
                    // Не является числом, значит это slug, но не найден
                }
            }
            
            if (minerDetailOpt.isEmpty()) {
                log.warn("Майнер не найден: idOrSlug={}", idOrSlug);
                // Устанавливаем статус 404 в запросе для обработки CustomErrorController
                request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.NOT_FOUND.value());
                request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, request.getRequestURI());
                // Перенаправляем на /error для обработки CustomErrorController
                return "forward:/error";
            }
            
            MinerDetail minerDetail = minerDetailOpt.get();
            
            // Устанавливаем URL изображения на основе standardName из MinerDetail
            // Используем imageUrl из MinerDetail, если указан, иначе используем ImageUrlResolver
            String imageUrl = null;
            if (minerDetail.getImageUrl() != null && !minerDetail.getImageUrl().trim().isEmpty()) {
                imageUrl = minerDetail.getImageUrl();
            } else {
                imageUrl = imageUrlResolver.resolveImageUrl(minerDetail.getStandardName());
            }
            
            // Определяем дату начала фильтрации
            LocalDateTime dateFrom = null;
            if (dateFilter != null && !dateFilter.isEmpty()) {
                LocalDateTime now = LocalDateTime.now();
                switch (dateFilter.toLowerCase()) {
                    case "today":
                        dateFrom = now.toLocalDate().atStartOfDay();
                        break;
                    case "3days":
                        dateFrom = now.minusDays(3);
                        break;
                    case "week":
                        dateFrom = now.minusWeeks(1);
                        break;
                    case "month":
                        dateFrom = now.minusMonths(1);
                        break;
                }
            }
            
            // Создаем Pageable с сортировкой
            Sort.Direction direction = sortDir.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Sort sort = Sort.by(direction, sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // Получаем ВСЕ предложения для ВСЕХ связанных товаров с пагинацией и фильтрацией
            Page<Offer> offersPage = productService.getOffersByMinerDetailIdWithFilters(minerDetail.getId(), dateFrom, null, null, pageable);
            List<Offer> offers = offersPage.getContent();
            
            // Разделяем предложения на продажи и покупки для статистики
            List<Offer> sellOffers = offers.stream()
                    .filter(o -> o.getOperationType() != null && o.getOperationType().name().equals("SELL"))
                    .toList();
            List<Offer> buyOffers = offers.stream()
                    .filter(o -> o.getOperationType() != null && o.getOperationType().name().equals("BUY"))
                    .toList();
            
            // Для расчета минимальной цены нужно получить все продажи (не только на странице)
            // Фильтруем только предложения на продажу (SELL) с ценой больше 0
            List<Offer> allSellOffers = productService.getOffersByMinerDetailId(minerDetail.getId()).stream()
                    .filter(o -> o.getOperationType() != null && o.getOperationType() == OperationType.SELL)
                    .filter(o -> o.getPrice() != null && o.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0)
                    .toList();
            
            // Вычисляем минимальную цену: сначала за последние 24 часа, потом за все время
            java.util.Optional<java.math.BigDecimal> minPrice = java.util.Optional.empty();
            String currency = null;
            
            if (!allSellOffers.isEmpty()) {
                // Сначала ищем предложения за последние 24 часа
                LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
                List<Offer> recentOffers = allSellOffers.stream()
                        .filter(o -> o.getUpdatedAt() != null && o.getUpdatedAt().isAfter(oneDayAgo))
                        .collect(java.util.stream.Collectors.toList());
                
                if (!recentOffers.isEmpty()) {
                    // Находим минимальную цену за последние 24 часа
                    minPrice = recentOffers.stream()
                            .map(Offer::getPrice)
                            .min(java.util.Comparator.naturalOrder());
                    
                    if (minPrice.isPresent()) {
                        final java.math.BigDecimal finalMinPrice = minPrice.get();
                        currency = recentOffers.stream()
                                .filter(o -> o.getPrice() != null && o.getPrice().equals(finalMinPrice))
                                .map(Offer::getCurrency)
                                .findFirst()
                                .orElse("RUB");
                    } else {
                        minPrice = java.util.Optional.empty();
                    }
                } else {
                    // Если за сутки нет предложений, ищем во всех предложениях
                    minPrice = allSellOffers.stream()
                            .map(Offer::getPrice)
                            .min(java.util.Comparator.naturalOrder());
                    
                    if (minPrice.isPresent()) {
                        final java.math.BigDecimal finalMinPrice = minPrice.get();
                        currency = allSellOffers.stream()
                                .filter(o -> o.getPrice() != null && o.getPrice().equals(finalMinPrice))
                                .map(Offer::getCurrency)
                                .findFirst()
                                .orElse("RUB");
                    }
                }
            }
            
            // Используем данные из MinerDetail для отображения
            String displayName = minerDetail.getStandardName() != null 
                    ? minerDetail.getStandardName() 
                    : "Майнер";
            String displayManufacturer = minerDetail.getManufacturer() != null 
                    ? minerDetail.getManufacturer() 
                    : "";
            
            // Для обратной совместимости создаем фиктивный Product объект
            Product product = new Product();
            product.setId(0L); // Не используется
            product.setModel(minerDetail.getStandardName());
            product.setManufacturer(minerDetail.getManufacturer());
            product.setImageUrl(imageUrl);
            
            model.addAttribute("product", product);
            model.addAttribute("minerDetail", minerDetail);
            model.addAttribute("displayName", displayName);
            model.addAttribute("displayManufacturer", displayManufacturer);
            model.addAttribute("offers", offers);
            model.addAttribute("offersPage", offersPage);
            model.addAttribute("sellOffers", sellOffers);
            model.addAttribute("buyOffers", buyOffers);
            // Сохраняем цену только если она не null и не 0
            if (minPrice.isPresent() && minPrice.get().compareTo(java.math.BigDecimal.ZERO) > 0) {
                model.addAttribute("minPrice", minPrice.get());
            } else {
                model.addAttribute("minPrice", null);
            }
            model.addAttribute("currency", currency != null ? currency : "RUB");
            model.addAttribute("dateFilter", dateFilter != null ? dateFilter : "");
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDir", sortDir);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            
            // Проверяем наличие CompanyMiner для этого MinerDetail
            Optional<CompanyMiner> companyMinerOpt = companyMinerRepository.findByMinerDetailIdAndActiveTrue(minerDetail.getId());
            boolean useCompanyMiner = companyMinerOpt.isPresent();
            CompanyMinerDTO.CompanyMinerInfo companyMinerInfo = null;
            
            if (useCompanyMiner) {
                companyMinerInfo = companyMinerService.getCompanyMinerByMinerDetailId(minerDetail.getId())
                        .orElse(null);
            }
            
            model.addAttribute("useCompanyMiner", useCompanyMiner);
            model.addAttribute("companyMiner", companyMinerInfo);
            
            // Schema.org разметка для страницы товара
            String productSchema = SchemaOrgUtil.generateProductSchema(minerDetail, offers, imageUrl);
            model.addAttribute("productSchema", productSchema);
            
            // Вычисляем slug один раз
            String slug = minerDetail.getSlug() != null && !minerDetail.getSlug().isEmpty() 
                    ? minerDetail.getSlug() 
                    : String.valueOf(minerDetail.getId());
            
            // Breadcrumb schema
            java.util.List<java.util.Map<String, String>> breadcrumbs = new java.util.ArrayList<>();
            java.util.Map<String, String> homeBreadcrumb = new java.util.HashMap<>();
            homeBreadcrumb.put("name", "Главная");
            homeBreadcrumb.put("url", "/");
            breadcrumbs.add(homeBreadcrumb);
            java.util.Map<String, String> productsBreadcrumb = new java.util.HashMap<>();
            productsBreadcrumb.put("name", "Товары");
            productsBreadcrumb.put("url", "/products");
            breadcrumbs.add(productsBreadcrumb);
            java.util.Map<String, String> productBreadcrumb = new java.util.HashMap<>();
            productBreadcrumb.put("name", minerDetail.getStandardName() != null ? minerDetail.getStandardName() : "Майнер");
            productBreadcrumb.put("url", "/products/" + slug);
            breadcrumbs.add(productBreadcrumb);
            model.addAttribute("breadcrumbSchema", SchemaOrgUtil.generateBreadcrumbSchema(breadcrumbs));
            
            // SEO мета-теги
            String pageTitle = minerDetail.getStandardName() != null 
                    ? "Майнер " + minerDetail.getStandardName() + " - MinerHive"
                    : "Майнер - MinerHive";
            String pageDescription = minerDetail.getDescription() != null && !minerDetail.getDescription().isEmpty()
                    ? minerDetail.getDescription()
                    : "Купить " + (minerDetail.getStandardName() != null ? minerDetail.getStandardName() : "майнер") + " для майнинга криптовалют. ASIC майнер от " + (minerDetail.getManufacturer() != null ? minerDetail.getManufacturer() : "производителя") + ".";
            String canonicalUrl = SeoUtil.generateCanonicalUrl("/products/" + slug);
            String ogImageUrl = imageUrl != null && !imageUrl.isEmpty() 
                    ? (imageUrl.startsWith("http") ? imageUrl : "https://minerhive.ru" + imageUrl)
                    : "https://minerhive.ru/assets/images/logo/logo.png";
            
            model.addAttribute("pageTitle", pageTitle);
            model.addAttribute("pageDescription", pageDescription);
            model.addAttribute("pageKeywords", (minerDetail.getStandardName() != null ? minerDetail.getStandardName() : "") + ", майнер, ASIC, " + (minerDetail.getManufacturer() != null ? minerDetail.getManufacturer() : "") + ", купить майнер");
            model.addAttribute("canonicalUrl", canonicalUrl);
            model.addAttribute("ogImage", ogImageUrl);
            
            return "product-details-new";
        } catch (ResponseStatusException e) {
            // Пробрасываем ResponseStatusException дальше для обработки GlobalExceptionHandler
            throw e;
        } catch (Exception e) {
            // Логируем ошибку для отладки
            log.error("Ошибка при загрузке детальной страницы майнера idOrSlug={}: {}", idOrSlug, e.getMessage(), e);
            model.addAttribute("error", "Произошла ошибка при загрузке страницы: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * REST API endpoint для получения предложений всех товаров, связанных с MinerDetail, в JSON формате (для AJAX)
     * @param id ID MinerDetail (не Product!)
     * @param dateFilter Фильтр по дате: today, 3days, week, month
     * @param sortBy Колонка для сортировки
     * @param sortDir Направление сортировки: ASC или DESC
     * @param page Номер страницы
     * @param size Размер страницы
     * @return JSON с предложениями и метаданными пагинации
     */
    @GetMapping(value = "/api/products/{id}/offers", produces = "application/json;charset=UTF-8")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getOffersJson(
            @PathVariable Long id,
            @RequestParam(required = false) String dateFilter,
            @RequestParam(required = false) String operationType, // SELL или BUY
            @RequestParam(required = false) Boolean hasPrice, // true для фильтра "Без пустых цен"
            @RequestParam(required = false, defaultValue = "updatedAt") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDir,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        try {
            // Проверяем, что MinerDetail существует
            log.info("Запрос предложений для MinerDetail ID={}", id);
            Optional<MinerDetail> minerDetailOpt = minerDetailRepository.findById(id);
            if (minerDetailOpt.isEmpty()) {
                log.warn("MinerDetail с ID={} не найден", id);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Майнер не найден");
                return ResponseEntity.status(404).body(error);
            }
            log.debug("MinerDetail с ID={} найден: {}", id, minerDetailOpt.get().getStandardName());
            
            // Определяем дату начала фильтрации
            LocalDateTime dateFrom = null;
            if (dateFilter != null && !dateFilter.isEmpty()) {
                LocalDateTime now = LocalDateTime.now();
                switch (dateFilter.toLowerCase()) {
                    case "today":
                        dateFrom = now.toLocalDate().atStartOfDay();
                        break;
                    case "3days":
                        dateFrom = now.minusDays(3);
                        break;
                    case "week":
                        dateFrom = now.minusWeeks(1);
                        break;
                    case "month":
                        dateFrom = now.minusMonths(1);
                        break;
                }
            }
            
            // Преобразуем operationType из строки в enum
            OperationType operationTypeEnum = null;
            if (operationType != null && !operationType.isEmpty()) {
                try {
                    operationTypeEnum = OperationType.valueOf(operationType.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Неизвестный тип операции: {}", operationType);
                }
            }
            
            // Создаем Pageable с сортировкой для использования в сервисе
            Sort.Direction direction = sortDir.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Sort sort = Sort.by(direction, sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // Получаем предложения всех связанных товаров с пагинацией и фильтрацией
            log.debug("Запрос предложений для MinerDetail ID={}, фильтр даты={}, тип операции={}, с ценой={}, сортировка={} {}, страница={}, размер={}", 
                    id, dateFilter, operationTypeEnum, hasPrice, sortBy, sortDir, page, size);
            
            // Используем новый метод для получения предложений по MinerDetail ID
            Page<Offer> offersPage = productService.getOffersByMinerDetailIdWithFilters(id, dateFrom, operationTypeEnum, hasPrice, pageable);
            
            log.debug("Получено предложений: {}", offersPage.getContent().size());
            
            // Преобразуем сущности в DTO для избежания циклических ссылок
            List<OfferDTO> offerDTOs = new java.util.ArrayList<>();
            try {
                for (Offer offer : offersPage.getContent()) {
                    try {
                        OfferDTO dto = OfferDTO.fromEntity(offer);
                        if (dto != null) {
                            offerDTOs.add(dto);
                        }
                    } catch (Exception ex) {
                        log.error("Ошибка при преобразовании Offer в DTO (ID={}): {}", offer.getId(), ex.getMessage(), ex);
                        // Продолжаем обработку остальных записей
                    }
                }
                log.debug("Преобразовано в DTO: {}", offerDTOs.size());
            } catch (Exception ex) {
                log.error("Критическая ошибка при преобразовании предложений в DTO: {}", ex.getMessage(), ex);
                throw ex;
            }
            
            // Формируем ответ
            Map<String, Object> response = new HashMap<>();
            response.put("content", offerDTOs);
            response.put("totalElements", offersPage.getTotalElements());
            response.put("totalPages", offersPage.getTotalPages());
            response.put("currentPage", offersPage.getNumber());
            response.put("pageSize", offersPage.getSize());
            response.put("first", offersPage.isFirst());
            response.put("last", offersPage.isLast());
            response.put("numberOfElements", offersPage.getNumberOfElements());
            
            log.debug("Ответ подготовлен, количество элементов: {}", offerDTOs.size());
            
            // Возвращаем ответ с явной установкой кодировки
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(response);
        } catch (Exception e) {
            log.error("Ошибка при получении предложений для MinerDetail ID={}: {}", id, e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Произошла ошибка при загрузке предложений: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * REST API endpoint для получения товаров (MinerDetails) в JSON формате (для AJAX)
     * @param page Номер страницы
     * @param size Размер страницы
     * @param sortBy Тип сортировки: latestOffer, name, manufacturer
     * @param manufacturer Список производителей для фильтрации
     * @param series Список серий для фильтрации
     * @return JSON с товарами и метаданными пагинации
     */
    @GetMapping(value = "/api/products", produces = "application/json;charset=UTF-8")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getProductsJson(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false, defaultValue = "latestOffer") String sortBy,
            @RequestParam(required = false) List<String> manufacturer,
            @RequestParam(required = false) List<String> series) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            
            // Получаем данные так же, как в методе products()
            Page<MinerDetail> minerDetailsPage = new org.springframework.data.domain.PageImpl<>(List.of());
            List<MinerDetail> filteredMinerDetails;
            
            if ((manufacturer == null || manufacturer.isEmpty()) && (series == null || series.isEmpty())) {
                filteredMinerDetails = minerDetailRepository.findAllWithOffers();
            } else {
                if (manufacturer != null && !manufacturer.isEmpty() && series != null && !series.isEmpty()) {
                    filteredMinerDetails = minerDetailRepository.findAllWithOffersByManufacturersAndSeries(manufacturer, series);
                } else if (manufacturer != null && !manufacturer.isEmpty()) {
                    filteredMinerDetails = minerDetailRepository.findAllWithOffersByManufacturers(manufacturer);
                } else {
                    filteredMinerDetails = minerDetailRepository.findAllWithOffersBySeries(series);
                }
            }
            
            // Применяем сортировку
            switch (sortBy) {
                case "name":
                    filteredMinerDetails.sort((md1, md2) -> {
                        String name1 = md1.getStandardName() != null ? md1.getStandardName() : "";
                        String name2 = md2.getStandardName() != null ? md2.getStandardName() : "";
                        return name1.compareToIgnoreCase(name2);
                    });
                    break;
                case "manufacturer":
                    filteredMinerDetails.sort((md1, md2) -> {
                        String man1 = md1.getManufacturer() != null ? md1.getManufacturer() : "";
                        String man2 = md2.getManufacturer() != null ? md2.getManufacturer() : "";
                        int cmp = man1.compareToIgnoreCase(man2);
                        if (cmp == 0) {
                            String name1 = md1.getStandardName() != null ? md1.getStandardName() : "";
                            String name2 = md2.getStandardName() != null ? md2.getStandardName() : "";
                            return name1.compareToIgnoreCase(name2);
                        }
                        return cmp;
                    });
                    break;
                case "latestOffer":
                default:
                    minerDetailsPage = minerDetailService.findAllSortedByLatestOfferUpdate(pageable, manufacturer, series);
                    filteredMinerDetails = new java.util.ArrayList<>();
                    break;
            }
            
            // Для сортировок "name" и "manufacturer" применяем пагинацию вручную
            if (!"latestOffer".equals(sortBy)) {
                if (filteredMinerDetails != null && !filteredMinerDetails.isEmpty()) {
                    int start = (int) pageable.getOffset();
                    int end = Math.min(start + pageable.getPageSize(), filteredMinerDetails.size());
                    
                    if (start >= filteredMinerDetails.size()) {
                        minerDetailsPage = new org.springframework.data.domain.PageImpl<>(List.of(), pageable, filteredMinerDetails.size());
                    } else {
                        if (start < 0) start = 0;
                        if (end <= start) end = Math.min(start + pageable.getPageSize(), filteredMinerDetails.size());
                        if (end > filteredMinerDetails.size()) end = filteredMinerDetails.size();
                        
                        try {
                            if (start >= 0 && start < filteredMinerDetails.size() && end > start && end <= filteredMinerDetails.size()) {
                                List<MinerDetail> pageContent = filteredMinerDetails.subList(start, end);
                                minerDetailsPage = new org.springframework.data.domain.PageImpl<>(pageContent, pageable, filteredMinerDetails.size());
                            } else {
                                minerDetailsPage = new org.springframework.data.domain.PageImpl<>(filteredMinerDetails, pageable, filteredMinerDetails.size());
                            }
                        } catch (IndexOutOfBoundsException e) {
                            log.error("Ошибка IndexOutOfBoundsException при создании подсписка: start={}, end={}, size={}", 
                                    start, end, filteredMinerDetails.size(), e);
                            minerDetailsPage = new org.springframework.data.domain.PageImpl<>(filteredMinerDetails, pageable, filteredMinerDetails.size());
                        }
                    }
                } else {
                    minerDetailsPage = new org.springframework.data.domain.PageImpl<>(List.of(), pageable, 0);
                }
            }
            
            List<MinerDetail> minerDetails = minerDetailsPage.getContent();
            
            // Собираем статистику для каждого MinerDetail
            Map<Long, ProductOperationInfo> minerDetailOperationInfo = new HashMap<>();
            Map<Long, String> imageUrls = new HashMap<>();
            
            for (MinerDetail minerDetail : minerDetails) {
                List<Product> linkedProducts = productRepository.findByMinerDetailId(minerDetail.getId());
                List<Long> linkedProductIds = linkedProducts.stream().map(Product::getId).toList();
                
                List<Offer> allMinerDetailOffers = new java.util.ArrayList<>();
                if (!linkedProductIds.isEmpty()) {
                    List<Offer> offers = offerRepository.findByProductIdIn(linkedProductIds);
                    offers.forEach(offer -> {
                        if (offer.getSeller() != null) {
                            offer.getSeller().getName();
                        }
                    });
                    allMinerDetailOffers.addAll(offers);
                }
                
                if (!allMinerDetailOffers.isEmpty()) {
                    long sellCount = allMinerDetailOffers.stream()
                            .filter(o -> o.getOperationType() != null && o.getOperationType() == OperationType.SELL)
                            .count();
                    long buyCount = allMinerDetailOffers.stream()
                            .filter(o -> o.getOperationType() != null && o.getOperationType() == OperationType.BUY)
                            .count();
                    
                    int totalQuantity = allMinerDetailOffers.stream()
                            .filter(o -> o.getQuantity() != null)
                            .mapToInt(Offer::getQuantity)
                            .sum();
                    
                    // Вычисляем минимальную цену: сначала за последние 24 часа, потом за все время
                    // Исключаем цены равные 0
                    java.util.Optional<java.math.BigDecimal> minPrice = java.util.Optional.empty();
                    String currency = null;
                    
                    // Фильтруем только предложения на продажу (SELL)
                    List<Offer> sellOffers = allMinerDetailOffers.stream()
                            .filter(o -> o.getOperationType() != null && o.getOperationType() == OperationType.SELL)
                            .filter(o -> o.getPrice() != null && o.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0)
                            .collect(java.util.stream.Collectors.toList());
                    
                    if (!sellOffers.isEmpty()) {
                        // Сначала ищем предложения за последние 24 часа
                        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
                        List<Offer> recentOffers = sellOffers.stream()
                                .filter(o -> o.getUpdatedAt() != null && o.getUpdatedAt().isAfter(oneDayAgo))
                                .collect(java.util.stream.Collectors.toList());
                        
                        if (!recentOffers.isEmpty()) {
                            // Находим минимальную цену за последние 24 часа
                            minPrice = recentOffers.stream()
                                    .map(Offer::getPrice)
                                    .min(java.util.Comparator.naturalOrder());
                            
                            if (minPrice.isPresent()) {
                                final java.math.BigDecimal finalMinPrice = minPrice.get();
                                currency = recentOffers.stream()
                                        .filter(o -> o.getPrice() != null && o.getPrice().equals(finalMinPrice))
                                        .map(Offer::getCurrency)
                                        .findFirst()
                                        .orElse("RUB");
                            } else {
                                minPrice = java.util.Optional.empty();
                            }
                        } else {
                            // Если за сутки нет предложений, ищем во всех предложениях
                            minPrice = sellOffers.stream()
                                    .map(Offer::getPrice)
                                    .min(java.util.Comparator.naturalOrder());
                            
                            if (minPrice.isPresent()) {
                                final java.math.BigDecimal finalMinPrice = minPrice.get();
                                currency = sellOffers.stream()
                                        .filter(o -> o.getPrice() != null && o.getPrice().equals(finalMinPrice))
                                        .map(Offer::getCurrency)
                                        .findFirst()
                                        .orElse("RUB");
                            }
                        }
                    } else {
                        minPrice = java.util.Optional.empty();
                    }
                    
                    ProductOperationInfo info = new ProductOperationInfo();
                    info.setHasSellOffers(sellCount > 0);
                    info.setHasBuyOffers(buyCount > 0);
                    info.setSellCount(sellCount);
                    info.setBuyCount(buyCount);
                    info.setTotalQuantity(totalQuantity);
                    // Сохраняем цену только если она не null и не 0
                    if (minPrice.isPresent() && minPrice.get().compareTo(java.math.BigDecimal.ZERO) > 0) {
                        info.setMinPrice(minPrice.get());
                    } else {
                        info.setMinPrice(null);
                    }
                    info.setCurrency(currency);
                    info.setManufacturer(minerDetail.getManufacturer());
                    
                    if (sellCount > 0 && buyCount == 0) {
                        info.setPrimaryOperationType(OperationType.SELL);
                    } else if (buyCount > 0 && sellCount == 0) {
                        info.setPrimaryOperationType(OperationType.BUY);
                    } else if (sellCount > 0 && buyCount > 0) {
                        info.setPrimaryOperationType(OperationType.SELL);
                    }
                    
                    minerDetailOperationInfo.put(minerDetail.getId(), info);
                }
                
                // Добавляем URL изображения: сначала проверяем imageUrl из MinerDetail, если нет - используем ImageUrlResolver
                String imageUrl = null;
                if (minerDetail.getImageUrl() != null && !minerDetail.getImageUrl().trim().isEmpty()) {
                    imageUrl = minerDetail.getImageUrl();
                } else {
                    imageUrl = imageUrlResolver.resolveImageUrl(minerDetail.getStandardName());
                }
                imageUrls.put(minerDetail.getId(), imageUrl);
            }
            
            // Преобразуем в DTO
            List<com.miners.shop.dto.MinerDetailDTO> minerDetailDTOs = minerDetails.stream()
                    .map(com.miners.shop.dto.MinerDetailDTO::fromEntity)
                    .toList();
            
            // Формируем ответ
            Map<String, Object> response = new HashMap<>();
            response.put("content", minerDetailDTOs);
            response.put("imageUrls", imageUrls);
            response.put("operationInfo", minerDetailOperationInfo.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            e -> e.getKey().toString(),
                            e -> {
                                Map<String, Object> info = new HashMap<>();
                                ProductOperationInfo opInfo = e.getValue();
                                info.put("hasSellOffers", opInfo.isHasSellOffers());
                                info.put("hasBuyOffers", opInfo.isHasBuyOffers());
                                info.put("sellCount", opInfo.getSellCount());
                                info.put("buyCount", opInfo.getBuyCount());
                                info.put("totalQuantity", opInfo.getTotalQuantity());
                                // Убеждаемся, что если цена равна 0 или null, передаем null
                                java.math.BigDecimal minPrice = opInfo.getMinPrice();
                                if (minPrice == null || minPrice.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                                    info.put("minPrice", null);
                                } else {
                                    info.put("minPrice", minPrice);
                                }
                                info.put("currency", opInfo.getCurrency());
                                info.put("manufacturer", opInfo.getManufacturer());
                                return info;
                            })));
            response.put("totalElements", minerDetailsPage.getTotalElements());
            response.put("totalPages", minerDetailsPage.getTotalPages());
            response.put("currentPage", minerDetailsPage.getNumber());
            response.put("pageSize", minerDetailsPage.getSize());
            response.put("first", minerDetailsPage.isFirst());
            response.put("last", minerDetailsPage.isLast());
            response.put("numberOfElements", minerDetailsPage.getNumberOfElements());
            
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(response);
        } catch (Exception e) {
            log.error("Ошибка при получении товаров: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Произошла ошибка при загрузке товаров: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * REST API endpoint для получения доступных серий по выбранным производителям (для AJAX)
     * @param manufacturers Список производителей
     * @return JSON со списком доступных серий
     */
    @GetMapping(value = "/api/products/series", produces = "application/json;charset=UTF-8")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getSeriesByManufacturers(
            @RequestParam(required = false) List<String> manufacturers) {
        try {
            List<String> availableSeries;
            if (manufacturers == null || manufacturers.isEmpty()) {
                availableSeries = minerDetailService.getDistinctSeries();
            } else {
                availableSeries = minerDetailService.getDistinctSeriesByManufacturers(manufacturers);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("series", availableSeries);
            
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(response);
        } catch (Exception e) {
            log.error("Ошибка при получении серий: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Произошла ошибка при загрузке серий: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * REST API endpoint для обновления связи Product с MinerDetail
     * @param request Тело запроса с productId и minerDetailId (может быть null для удаления связи)
     * @return Результат операции
     */
    @PostMapping(value = "/api/products/update-miner-detail", produces = "application/json;charset=UTF-8")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> updateProductMinerDetail(@RequestBody Map<String, Object> request) {
        try {
            log.info("Запрос на обновление связи Product с MinerDetail: {}", request);
            
            // Получаем параметры из запроса
            Object productIdObj = request.get("productId");
            Object minerDetailIdObj = request.get("minerDetailId");
            
            if (productIdObj == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "ID продукта обязателен");
                return ResponseEntity.badRequest().body(error);
            }
            
            Long productId;
            try {
                productId = Long.parseLong(productIdObj.toString());
            } catch (NumberFormatException e) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Неверный формат ID продукта");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Получаем продукт
            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Продукт с ID=" + productId + " не найден");
                return ResponseEntity.status(404).body(error);
            }
            
            Product product = productOpt.get();
            
            // Обрабатываем MinerDetail
            MinerDetail minerDetail = null;
            if (minerDetailIdObj != null) {
                try {
                    Long minerDetailId = Long.parseLong(minerDetailIdObj.toString());
                    Optional<MinerDetail> minerDetailOpt = minerDetailRepository.findById(minerDetailId);
                    if (minerDetailOpt.isEmpty()) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("success", false);
                        error.put("error", "MinerDetail с ID=" + minerDetailId + " не найден");
                        return ResponseEntity.status(404).body(error);
                    }
                    minerDetail = minerDetailOpt.get();
                } catch (NumberFormatException e) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("error", "Неверный формат ID MinerDetail");
                    return ResponseEntity.badRequest().body(error);
                }
            }
            
            // Обновляем связь
            MinerDetail oldMinerDetail = product.getMinerDetail();
            product.setMinerDetail(minerDetail);
            productRepository.save(product);
            
            String oldName = oldMinerDetail != null ? oldMinerDetail.getStandardName() : "нет";
            String newName = minerDetail != null ? minerDetail.getStandardName() : "нет";
            
            log.info("Связь Product ID={} ({}) обновлена: {} -> {}", 
                    productId, product.getModel(), oldName, newName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Связь успешно обновлена");
            response.put("productId", productId);
            response.put("minerDetailId", minerDetail != null ? minerDetail.getId() : null);
            response.put("minerDetailName", minerDetail != null ? minerDetail.getStandardName() : null);
            
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(response);
                    
        } catch (Exception e) {
            log.error("Ошибка при обновлении связи Product с MinerDetail: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Ошибка при обновлении связи: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * API endpoint для получения MinerDetails с пагинацией, поиском и количеством товаров
     * Используется в модальном окне для выбора MinerDetail
     */
    @GetMapping(value = "/api/miner-details/search", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    @ResponseBody
    @Transactional(readOnly = true)
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> searchMinerDetails(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        try {
            log.debug("Поиск MinerDetails: страница={}, размер={}, поиск={}", page, size, search);
            
            // Создаем Pageable
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "standardName"));
            
            // Нормализуем поисковый запрос
            String searchQuery = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
            
            // Получаем MinerDetails с пагинацией и поиском
            Page<MinerDetail> minerDetailsPage = minerDetailRepository.findAllBySearchOrderByStandardNameAsc(
                    searchQuery, pageable);
            
            // Подсчитываем количество товаров для каждого MinerDetail
            List<Map<String, Object>> minerDetailsData = new java.util.ArrayList<>();
            for (MinerDetail md : minerDetailsPage.getContent()) {
                // Подсчитываем количество связанных товаров
                long productCount = productRepository.findByMinerDetailId(md.getId()).size();
                
                Map<String, Object> mdData = new HashMap<>();
                mdData.put("id", md.getId());
                mdData.put("standardName", md.getStandardName());
                mdData.put("manufacturer", md.getManufacturer());
                mdData.put("series", md.getSeries());
                mdData.put("productCount", productCount);
                
                minerDetailsData.add(mdData);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", minerDetailsData);
            response.put("totalElements", minerDetailsPage.getTotalElements());
            response.put("totalPages", minerDetailsPage.getTotalPages());
            response.put("currentPage", minerDetailsPage.getNumber());
            response.put("pageSize", minerDetailsPage.getSize());
            response.put("hasNext", minerDetailsPage.hasNext());
            response.put("hasPrevious", minerDetailsPage.hasPrevious());
            
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(response);
                    
        } catch (Exception e) {
            log.error("Ошибка при поиске MinerDetails: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Ошибка при поиске: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Вспомогательный класс для хранения информации о типе операции продукта
     */
    public static class ProductOperationInfo {
        private boolean hasSellOffers = false;
        private boolean hasBuyOffers = false;
        private long sellCount = 0;
        private long buyCount = 0;
        private OperationType primaryOperationType = null;
        private int totalQuantity = 0;
        private java.math.BigDecimal minPrice = null;
        private String currency = null;
        private String manufacturer = null;
        
        public boolean isHasSellOffers() {
            return hasSellOffers;
        }
        
        public void setHasSellOffers(boolean hasSellOffers) {
            this.hasSellOffers = hasSellOffers;
        }
        
        public boolean isHasBuyOffers() {
            return hasBuyOffers;
        }
        
        public void setHasBuyOffers(boolean hasBuyOffers) {
            this.hasBuyOffers = hasBuyOffers;
        }
        
        public long getSellCount() {
            return sellCount;
        }
        
        public void setSellCount(long sellCount) {
            this.sellCount = sellCount;
        }
        
        public long getBuyCount() {
            return buyCount;
        }
        
        public void setBuyCount(long buyCount) {
            this.buyCount = buyCount;
        }
        
        public OperationType getPrimaryOperationType() {
            return primaryOperationType;
        }
        
        public void setPrimaryOperationType(OperationType primaryOperationType) {
            this.primaryOperationType = primaryOperationType;
        }
        
        public int getTotalQuantity() {
            return totalQuantity;
        }
        
        public void setTotalQuantity(int totalQuantity) {
            this.totalQuantity = totalQuantity;
        }
        
        public java.math.BigDecimal getMinPrice() {
            return minPrice;
        }
        
        public void setMinPrice(java.math.BigDecimal minPrice) {
            this.minPrice = minPrice;
        }
        
        public String getCurrency() {
            return currency;
        }
        
        public void setCurrency(String currency) {
            this.currency = currency;
        }
        
        public String getManufacturer() {
            return manufacturer;
        }
        
        public void setManufacturer(String manufacturer) {
            this.manufacturer = manufacturer;
        }
    }
}

