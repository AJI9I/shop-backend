package com.miners.shop.controller;

import com.miners.shop.dto.OfferDTO;
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
    private final com.miners.shop.util.ImageUrlResolver imageUrlResolver;
    
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
     */
    @GetMapping("/products")
    @Transactional(readOnly = true)
    public String products(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,  // Увеличено до 12 для карточек
            Model model) {
        try {
            // Создаем Pageable с сортировкой по дате обновления (новые сначала)
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
            
            // Получаем товары без eagerly загруженных offers (избегаем проблем с JOIN FETCH и пагинацией)
            Page<Product> products = productRepository.findAll(pageable);
            
            // Получаем ID всех продуктов на текущей странице
            List<Long> productIds = products.getContent().stream()
                    .map(Product::getId)
                    .toList();
            
            // Загружаем все offers для этих продуктов одним запросом
            final Map<Long, List<Offer>> offersByProductId;
            if (!productIds.isEmpty()) {
                List<Offer> allOffers = offerRepository.findByProductIdIn(productIds);
                offersByProductId = allOffers.stream()
                        .collect(java.util.stream.Collectors.groupingBy(o -> o.getProduct().getId()));
                
                // Назначаем offers каждому продукту
                products.getContent().forEach(product -> {
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
            } else {
                offersByProductId = new HashMap<>();
            }
            
            // Определяем тип операции для каждого продукта
            Map<Long, ProductOperationInfo> productOperationInfo = new HashMap<>();
            
            products.getContent().forEach(product -> {
                // Получаем коллекцию offers (уже загружена)
                List<Offer> productOffers = product.getOffers();
                if (productOffers != null && !productOffers.isEmpty()) {
                    // Определяем тип операции для продукта
                    long sellCount = productOffers.stream()
                            .filter(o -> o.getOperationType() != null && o.getOperationType() == OperationType.SELL)
                            .count();
                    long buyCount = productOffers.stream()
                            .filter(o -> o.getOperationType() != null && o.getOperationType() == OperationType.BUY)
                            .count();
                    
                    // Рассчитываем общее количество
                    int totalQuantity = productOffers.stream()
                            .filter(o -> o.getQuantity() != null)
                            .mapToInt(offer -> offer.getQuantity())
                            .sum();
                    
                    // Находим минимальную цену для продажи
                    java.util.Optional<java.math.BigDecimal> minPrice = productOffers.stream()
                            .filter(o -> o.getOperationType() != null && o.getOperationType() == OperationType.SELL)
                            .filter(o -> o.getPrice() != null)
                            .map(offer -> offer.getPrice())
                            .min(java.util.Comparator.naturalOrder());
                    
                    // Находим валюту минимальной цены
                    String currency = null;
                    if (minPrice.isPresent()) {
                        currency = productOffers.stream()
                                .filter(o -> o.getOperationType() != null && o.getOperationType() == OperationType.SELL)
                                .filter(o -> o.getPrice() != null && o.getPrice().equals(minPrice.get()))
                                .map(offer -> offer.getCurrency())
                                .findFirst()
                                .orElse("RUB");
                    }
                    
                    // Находим производителя из Product (если есть), иначе из первого предложения
                    String manufacturer = product.getManufacturer();
                    if (manufacturer == null || manufacturer.isEmpty()) {
                        manufacturer = productOffers.stream()
                                .filter(o -> o.getManufacturer() != null && !o.getManufacturer().isEmpty())
                                .map(com.miners.shop.entity.Offer::getManufacturer)
                                .findFirst()
                                .orElse(null);
                        // Если нашли в предложении, сохраняем в Product
                        if (manufacturer != null && !manufacturer.isEmpty()) {
                            product.setManufacturer(manufacturer);
                            // Сохраняем в Product (синхронно, но быстро)
                            try {
                                productRepository.save(product);
                            } catch (Exception e) {
                                // Логируем, но не прерываем выполнение
                                log.warn("Ошибка при обновлении manufacturer в Product: {}", e.getMessage());
                            }
                        }
                    }
                    
                    ProductOperationInfo info = new ProductOperationInfo();
                    info.setHasSellOffers(sellCount > 0);
                    info.setHasBuyOffers(buyCount > 0);
                    info.setSellCount(sellCount);
                    info.setBuyCount(buyCount);
                    info.setTotalQuantity(totalQuantity);
                    info.setMinPrice(minPrice.orElse(null));
                    info.setCurrency(currency);
                    info.setManufacturer(manufacturer);
                    
                    // Определяем основной тип операции
                    if (sellCount > 0 && buyCount == 0) {
                        info.setPrimaryOperationType(OperationType.SELL);
                    } else if (buyCount > 0 && sellCount == 0) {
                        info.setPrimaryOperationType(OperationType.BUY);
                    } else if (sellCount > 0 && buyCount > 0) {
                        // Если есть оба типа, приоритет продаже (так как продажи важнее для магазина)
                        info.setPrimaryOperationType(OperationType.SELL);
                    }
                    
                    productOperationInfo.put(product.getId(), info);
                    
                    // Устанавливаем URL изображения на основе модели
                    String imageUrl = imageUrlResolver.resolveImageUrl(product.getModel());
                    product.setImageUrl(imageUrl);
                } else {
                    // Если у продукта нет offers, устанавливаем пустую информацию
                    product.setImageUrl(imageUrlResolver.resolveImageUrl(product.getModel()));
                }
            });
            
            // Статистика
            long totalProducts = productService.getTotalProducts();
            long totalOffers = productService.getTotalOffers();
            
            model.addAttribute("products", products);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalProducts", totalProducts);
            model.addAttribute("totalOffers", totalOffers);
            model.addAttribute("productOperationInfo", productOperationInfo);
            
            return "products-new";
        } catch (Exception e) {
            // Логируем ошибку для отладки
            System.err.println("Ошибка при загрузке страницы товаров: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Произошла ошибка при загрузке страницы: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Страница детальной информации о товаре (модели майнера)
     * Показывает все предложения по этой модели
     */
    @GetMapping("/products/{id}")
    public String productDetails(
            @PathVariable Long id,
            @RequestParam(required = false) String dateFilter, // today, 3days, week, month
            @RequestParam(required = false, defaultValue = "updatedAt") String sortBy, // Колонка для сортировки
            @RequestParam(required = false, defaultValue = "DESC") String sortDir, // Направление сортировки
            @RequestParam(required = false, defaultValue = "0") int page, // Страница
            @RequestParam(required = false, defaultValue = "10") int size, // По умолчанию 10 записей при первой загрузке // Количество записей на странице
            Model model) {
        try {
            // Получаем товар
            Optional<Product> productOpt = productService.getProductById(id);
            
            if (productOpt.isEmpty()) {
                model.addAttribute("error", "Товар не найден");
                return "error";
            }
            
            Product product = productOpt.get();
            
            // Устанавливаем URL изображения на основе модели
            String imageUrl = imageUrlResolver.resolveImageUrl(product.getModel());
            product.setImageUrl(imageUrl);
            
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
            
            // Получаем предложения с пагинацией и фильтрацией
            Page<Offer> offersPage = productService.getOffersByProductIdWithPagination(id, dateFrom, pageable);
            List<Offer> offers = offersPage.getContent();
            
            // Разделяем предложения на продажи и покупки для статистики
            var sellOffers = offers.stream()
                    .filter(o -> o.getOperationType() != null && o.getOperationType().name().equals("SELL"))
                    .toList();
            var buyOffers = offers.stream()
                    .filter(o -> o.getOperationType() != null && o.getOperationType().name().equals("BUY"))
                    .toList();
            
            // Для расчета минимальной цены нужно получить все продажи (не только на странице)
            var allSellOffers = productService.getOffersByProductId(id).stream()
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
            
            model.addAttribute("product", product);
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
            System.err.println("Ошибка при загрузке детальной страницы товара: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Произошла ошибка при загрузке страницы: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * REST API endpoint для получения предложений товара в JSON формате (для AJAX)
     * @param id ID товара
     * @param dateFilter Фильтр по дате: today, 3days, week, month
     * @param sortBy Колонка для сортировки
     * @param sortDir Направление сортировки: ASC или DESC
     * @param page Номер страницы
     * @param size Размер страницы
     * @return JSON с предложениями и метаданными пагинации
     */
    @GetMapping(value = "/api/products/{id}/offers", produces = "application/json;charset=UTF-8")
    @ResponseBody
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
            
            // Получаем предложения с пагинацией и фильтрацией
            log.debug("Запрос предложений для товара ID={}, фильтр даты={}, тип операции={}, с ценой={}, сортировка={} {}, страница={}, размер={}", 
                    id, dateFilter, operationTypeEnum, hasPrice, sortBy, sortDir, page, size);
            
            // Используем новый метод с поддержкой всех фильтров и динамической сортировкой
            Page<Offer> offersPage = productService.getOffersByProductIdWithFilters(id, dateFrom, operationTypeEnum, hasPrice, pageable);
            
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
            log.error("Ошибка при получении предложений для товара ID={}: {}", id, e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Произошла ошибка при загрузке предложений: " + e.getMessage());
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

