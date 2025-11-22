package com.miners.shop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miners.shop.dto.OfferDTO;
import com.miners.shop.entity.MinerDetail;
import com.miners.shop.entity.OperationType;
import com.miners.shop.entity.Offer;
import com.miners.shop.entity.Product;
import com.miners.shop.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    
    /**
     * Страница с таблицей всех продуктов
     * Отображает все товары в виде таблицы с подробной информацией
     */
    @GetMapping("/products/table")
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
                
                // Устанавливаем URL изображения
                String imageUrl = imageUrlResolver.resolveImageUrl(product.getModel());
                product.setImageUrl(imageUrl);
            });
            
            model.addAttribute("productsPage", productsPage);
            model.addAttribute("currentPage", page);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDir", sortDir);
            model.addAttribute("manufacturers", manufacturers);
            model.addAttribute("currentManufacturer", manufacturer != null ? manufacturer : "");
            
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
            Model model) {
        try {
            Page<MinerDetail> minerDetailsPage = new org.springframework.data.domain.PageImpl<>(List.of());
            Pageable pageable = PageRequest.of(page, size);
            
            // Выбираем метод сортировки в зависимости от параметра
            // ВАЖНО: Все типы сортировки должны учитывать фильтры по manufacturer и series
            
            // Получаем все MinerDetail с фильтрами (если есть)
            List<MinerDetail> filteredMinerDetails;
            if ((manufacturer == null || manufacturer.isEmpty()) && (series == null || series.isEmpty())) {
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
                    // Используем метод сервиса для сортировки по последнему обновлению предложений
                    // В этом случае уже есть фильтрация в сервисе
                    minerDetailsPage = minerDetailService.findAllSortedByLatestOfferUpdate(pageable, manufacturer, series);
                    // Переходим к обработке результатов, filteredMinerDetails не используется
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
            
            // Для каждого MinerDetail находим все связанные Product и собираем их offers
            Map<Long, List<Offer>> allOffersByMinerDetailId = new HashMap<>();
            Map<Long, ProductOperationInfo> minerDetailOperationInfo = new HashMap<>();
            
            for (MinerDetail minerDetail : minerDetails) {
                // Получаем все Product, связанные с этим MinerDetail
                List<Product> linkedProducts = productRepository.findByMinerDetailId(minerDetail.getId());
                
                // Собираем все ID связанных Product
                List<Long> linkedProductIds = linkedProducts.stream()
                        .map(Product::getId)
                        .toList();
                
                // Загружаем все offers для всех связанных Product одним запросом
                List<Offer> allMinerDetailOffers = new java.util.ArrayList<>();
                if (!linkedProductIds.isEmpty()) {
                    List<Offer> offers = offerRepository.findByProductIdIn(linkedProductIds);
                    // Инициализируем продавцов
                    offers.forEach(offer -> {
                        if (offer.getSeller() != null) {
                            offer.getSeller().getName();
                        }
                    });
                    allMinerDetailOffers.addAll(offers);
                }
                
                allOffersByMinerDetailId.put(minerDetail.getId(), allMinerDetailOffers);
                
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
                    
                    java.util.Optional<java.math.BigDecimal> minPrice = allMinerDetailOffers.stream()
                            .filter(o -> o.getOperationType() != null && o.getOperationType() == OperationType.SELL)
                            .filter(o -> o.getPrice() != null)
                            .map(Offer::getPrice)
                            .min(java.util.Comparator.naturalOrder());
                    
                    String currency = null;
                    if (minPrice.isPresent()) {
                        currency = allMinerDetailOffers.stream()
                                .filter(o -> o.getOperationType() != null && o.getOperationType() == OperationType.SELL)
                                .filter(o -> o.getPrice() != null && o.getPrice().equals(minPrice.get()))
                                .map(Offer::getCurrency)
                                .findFirst()
                                .orElse("RUB");
                    }
                    
                    ProductOperationInfo info = new ProductOperationInfo();
                    info.setHasSellOffers(sellCount > 0);
                    info.setHasBuyOffers(buyCount > 0);
                    info.setSellCount(sellCount);
                    info.setBuyCount(buyCount);
                    info.setTotalQuantity(totalQuantity);
                    info.setMinPrice(minPrice.orElse(null));
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
            
            // Для каждой MinerDetail добавляем URL изображения (на основе standardName)
            Map<Long, String> imageUrls = new HashMap<>();
            minerDetailDTOs.forEach(dto -> {
                String imageUrl = imageUrlResolver.resolveImageUrl(dto.getStandardName());
                imageUrls.put(dto.getId(), imageUrl);
            });
            
            model.addAttribute("minerDetails", minerDetailDTOs);
            model.addAttribute("imageUrls", imageUrls);
            model.addAttribute("minerDetailsPage", minerDetailsPage);
            model.addAttribute("currentPage", page);
            model.addAttribute("currentSortBy", sortBy);
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
    @GetMapping("/products/{id}")
    @Transactional(readOnly = true)
    public String productDetails(
            @PathVariable Long id,
            @RequestParam(required = false) String dateFilter, // today, 3days, week, month
            @RequestParam(required = false, defaultValue = "updatedAt") String sortBy, // Колонка для сортировки
            @RequestParam(required = false, defaultValue = "DESC") String sortDir, // Направление сортировки
            @RequestParam(required = false, defaultValue = "0") int page, // Страница
            @RequestParam(required = false, defaultValue = "10") int size, // По умолчанию 10 записей при первой загрузке // Количество записей на странице
            Model model) {
        try {
            // Получаем MinerDetail по ID (id теперь это ID MinerDetail, а не Product)
            Optional<MinerDetail> minerDetailOpt = minerDetailRepository.findById(id);
            
            if (minerDetailOpt.isEmpty()) {
                model.addAttribute("error", "Майнер не найден");
                return "error";
            }
            
            MinerDetail minerDetail = minerDetailOpt.get();
            
            // Устанавливаем URL изображения на основе standardName из MinerDetail
            String imageUrl = imageUrlResolver.resolveImageUrl(minerDetail.getStandardName());
            
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
            Page<Offer> offersPage = productService.getOffersByMinerDetailIdWithFilters(id, dateFrom, null, null, pageable);
            List<Offer> offers = offersPage.getContent();
            
            // Разделяем предложения на продажи и покупки для статистики
            var sellOffers = offers.stream()
                    .filter(o -> o.getOperationType() != null && o.getOperationType().name().equals("SELL"))
                    .toList();
            var buyOffers = offers.stream()
                    .filter(o -> o.getOperationType() != null && o.getOperationType().name().equals("BUY"))
                    .toList();
            
            // Для расчета минимальной цены нужно получить все продажи (не только на странице)
            var allSellOffers = productService.getOffersByMinerDetailId(id).stream()
                    .filter(o -> o.getOperationType() != null && o.getOperationType().name().equals("SELL"))
                    .filter(o -> o.getPrice() != null)
                    .toList();
            
            // Рассчитываем минимальную цену для продажи
            java.util.Optional<java.math.BigDecimal> minPrice = allSellOffers.stream()
                    .map(offer -> offer.getPrice())
                    .min(java.util.Comparator.naturalOrder());
            
            // Находим валюту минимальной цены
            String currency = null;
            if (minPrice.isPresent()) {
                currency = allSellOffers.stream()
                        .filter(o -> o.getPrice() != null && o.getPrice().equals(minPrice.get()))
                        .map(offer -> offer.getCurrency())
                        .findFirst()
                        .orElse("RUB");
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
            model.addAttribute("minPrice", minPrice.orElse(null));
            model.addAttribute("currency", currency != null ? currency : "RUB");
            model.addAttribute("dateFilter", dateFilter != null ? dateFilter : "");
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDir", sortDir);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            
            return "product-details-new";
        } catch (Exception e) {
            // Логируем ошибку для отладки
            log.error("Ошибка при загрузке детальной страницы майнера ID={}: {}", id, e.getMessage(), e);
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
                    
                    java.util.Optional<java.math.BigDecimal> minPrice = allMinerDetailOffers.stream()
                            .filter(o -> o.getOperationType() != null && o.getOperationType() == OperationType.SELL)
                            .filter(o -> o.getPrice() != null)
                            .map(Offer::getPrice)
                            .min(java.util.Comparator.naturalOrder());
                    
                    String currency = null;
                    if (minPrice.isPresent()) {
                        currency = allMinerDetailOffers.stream()
                                .filter(o -> o.getOperationType() != null && o.getOperationType() == OperationType.SELL)
                                .filter(o -> o.getPrice() != null && o.getPrice().equals(minPrice.get()))
                                .map(Offer::getCurrency)
                                .findFirst()
                                .orElse("RUB");
                    }
                    
                    ProductOperationInfo info = new ProductOperationInfo();
                    info.setHasSellOffers(sellCount > 0);
                    info.setHasBuyOffers(buyCount > 0);
                    info.setSellCount(sellCount);
                    info.setBuyCount(buyCount);
                    info.setTotalQuantity(totalQuantity);
                    info.setMinPrice(minPrice.orElse(null));
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
                
                // Добавляем URL изображения
                imageUrls.put(minerDetail.getId(), imageUrlResolver.resolveImageUrl(minerDetail.getStandardName()));
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
                                info.put("minPrice", opInfo.getMinPrice());
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

