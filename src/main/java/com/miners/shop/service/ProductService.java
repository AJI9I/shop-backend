package com.miners.shop.service;

import com.miners.shop.entity.MinerDetail;
import com.miners.shop.entity.Offer;
import com.miners.shop.entity.OperationType;
import com.miners.shop.entity.Product;
import com.miners.shop.entity.Seller;
import com.miners.shop.repository.OfferRepository;
import com.miners.shop.repository.ProductRepository;
import com.miners.shop.service.SellerService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Сервис для работы с товарами и предложениями
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    private final ProductRepository productRepository;
    private final OfferRepository offerRepository;
    private final SellerService sellerService;
    private final com.miners.shop.service.MinerDetailService minerDetailService;
    
    /**
     * Обрабатывает распарсенные данные от Ollama и сохраняет товары с предложениями
     * При обнаружении дубликатов обновляет существующие предложения вместо создания новых
     * @param parsedData - Распарсенные данные от Ollama
     * @param messageId - ID сообщения из WhatsApp
     * @param chatName - Название чата
     * @param sellerName - Имя продавца
     * @param sellerPhone - Телефон продавца
     * @param location - Локация продажи
     * @return true, если это обновление существующих предложений, false если новые предложения
     */
    @Transactional
    public boolean processParsedData(Map<String, Object> parsedData, String messageId, 
                                  String chatName, String sellerName, String sellerPhone, String location) {
        if (parsedData == null || parsedData.isEmpty()) {
            log.debug("Распарсенные данные пусты, пропускаем обработку");
            return false;
        }
        
        log.info("Обработка распарсенных данных от Ollama для сообщения: {}", messageId);
        
        // Извлекаем тип операции (по умолчанию SELL)
        Object operationTypeObj = parsedData.get("operationType");
        OperationType operationType = OperationType.SELL; // По умолчанию продажа
        
        if (operationTypeObj != null) {
            try {
                String operationTypeStr = operationTypeObj.toString().toUpperCase();
                operationType = OperationType.valueOf(operationTypeStr);
                log.info("✅ Определен тип операции из Ollama: {} (исходное значение: {})", operationType, operationTypeObj);
            } catch (IllegalArgumentException e) {
                log.warn("⚠️  Неизвестный тип операции: {}, используем SELL по умолчанию", operationTypeObj);
            }
        } else {
            log.warn("⚠️  Тип операции не указан в ответе Ollama, используем SELL по умолчанию");
        }
        
        // Извлекаем список товаров
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> products = (List<Map<String, Object>>) parsedData.get("products");
        
        if (products == null || products.isEmpty()) {
            log.debug("Список товаров пуст в распарсенных данных");
            return false;
        }
        
        log.info("📦 Найдено товаров для обработки: {}", products.size());
        for (int i = 0; i < products.size(); i++) {
            Map<String, Object> p = products.get(i);
            String model = (String) p.get("model");
            String loc = (String) p.get("location");
            log.info("  {}: {} (локация: {})", i + 1, model != null ? model : "N/A", loc != null ? loc : "N/A");
        }
        
        // Используем локацию из распарсенных данных, если не передана
        if (location == null || location.isEmpty()) {
            location = (String) parsedData.get("location");
        }
        
        // Находим или создаем продавца
        Seller seller = null;
        if (sellerPhone != null && !sellerPhone.isEmpty()) {
            seller = sellerService.findOrCreateSeller(sellerPhone, sellerName, null);
        }
        
        // Проверяем, есть ли уже предложения от этого продавца (для определения дубликатов)
        // Считаем, что это обновление, если есть хотя бы одно предложение от этого продавца
        boolean isUpdate = seller != null && !offerRepository.findBySellerId(seller.getId()).isEmpty();
        
        int updatedCount = 0;
        int createdCount = 0;
        
        // Обрабатываем каждый товар
        for (int i = 0; i < products.size(); i++) {
            Map<String, Object> productData = products.get(i);
            try {
                // Если у товара есть своя локация, используем её, иначе используем общую локацию
                String productLocation = (String) productData.get("location");
                if (productLocation == null || productLocation.isEmpty()) {
                    productLocation = location;
                }
                
                String model = (String) productData.get("model");
                log.info("🔄 Обработка товара {}/{}: {} (локация: {})", i + 1, products.size(), model != null ? model : "N/A", productLocation);
                
                boolean wasUpdated = processProduct(productData, messageId, chatName, seller, productLocation, operationType, isUpdate);
                if (wasUpdated) {
                    updatedCount++;
                    log.info("✅ Товар {} обновлен", model);
                } else {
                    createdCount++;
                    log.info("✅ Товар {} создан", model);
                }
            } catch (Exception e) {
                log.error("❌ Ошибка при обработке товара {}/{}: {}", i + 1, products.size(), e.getMessage(), e);
            }
        }
        
        log.info("Обработано {} товаров из распарсенных данных: {} обновлено, {} создано", 
                products.size(), updatedCount, createdCount);
        
        return updatedCount > 0;
    }
    
    /**
     * Обрабатывает один товар и создает/обновляет предложение
     * Если уже есть предложение от этого продавца для этой модели - обновляет его
     * @return true, если предложение было обновлено, false если создано новое
     */
    private boolean processProduct(Map<String, Object> productData, String messageId, 
                               String chatName, Seller seller, String location,
                               OperationType operationType, boolean checkForDuplicates) {
        String model = (String) productData.get("model");
        if (model == null || model.isEmpty()) {
            log.warn("Модель товара не указана, пропускаем");
            return false;
        }
        
        // Находим или создаем товар
        Product product = productRepository.findByModel(model)
                .orElseGet(() -> {
                    Product newProduct = new Product();
                    newProduct.setModel(model);
                    newProduct.setDescription((String) productData.get("description"));
                    // Сохраняем производителя из Ollama при создании нового товара
                    String manufacturer = (String) productData.get("manufacturer");
                    if (manufacturer != null && !manufacturer.trim().isEmpty()) {
                        newProduct.setManufacturer(manufacturer.trim());
                        log.info("✅ Установлен производитель для нового товара {}: {}", model, manufacturer.trim());
                    } else {
                        log.debug("⚠️  Производитель не указан в данных Ollama для нового товара: {}", model);
                    }
                    Product savedProduct = productRepository.save(newProduct);
                    log.info("➕ Создан новый товар: {} (ID: {})", model, savedProduct.getId());
                    
                    // Автоматически создаем детальную запись для нового товара
                    try {
                        if (savedProduct.getMinerDetail() == null) {
                            MinerDetail minerDetail = minerDetailService.createMinerDetailForProduct(savedProduct);
                            savedProduct.setMinerDetail(minerDetail);
                            productRepository.save(savedProduct);
                            log.info("✅ Создана детальная запись для нового товара {}: MinerDetail ID={}", 
                                    model, minerDetail.getId());
                        }
                    } catch (Exception e) {
                        log.error("❌ Ошибка при создании детальной записи для товара {}: {}", 
                                model, e.getMessage(), e);
                        // Не прерываем выполнение, если ошибка в создании детальной записи
                    }
                    
                    return savedProduct;
                });
        
        // Обновляем производителя в существующем товаре, если он еще не заполнен
        if (product.getManufacturer() == null || product.getManufacturer().trim().isEmpty()) {
            String manufacturer = (String) productData.get("manufacturer");
            if (manufacturer != null && !manufacturer.trim().isEmpty()) {
                product.setManufacturer(manufacturer.trim());
                productRepository.save(product);
                log.info("✅ Обновлен производитель для существующего товара {}: {} -> {}", 
                        model, product.getManufacturer() != null ? product.getManufacturer() : "null", manufacturer.trim());
            } else {
                log.debug("⚠️  Производитель не указан в данных Ollama для существующего товара: {} (текущий manufacturer: {})", 
                        model, product.getManufacturer());
            }
        } else {
            log.debug("ℹ️  Производитель уже заполнен для товара {}: {}", model, product.getManufacturer());
        }
        
        log.debug("Товар найден/создан: {} (ID: {})", model, product.getId());
        
        // Проверяем продавца
        if (seller == null) {
            log.warn("⚠️  Продавец не передан в метод, пропускаем товар: {}", model);
            return false;
        }
        
        // Ищем существующее предложение от этого продавца для этой модели
        // Учитываем: продукт + продавец + тип операции (SELL/BUY)
        // Это позволяет продавцу иметь отдельные предложения для продажи и покупки одной модели
        Offer existingOffer = null;
        if (checkForDuplicates && seller != null) {
            List<Offer> existingOffers = offerRepository.findByProductIdAndSellerId(product.getId(), seller.getId());
            if (!existingOffers.isEmpty()) {
                // Фильтруем по типу операции - продавец может продавать И покупать одну модель
                // Это разные предложения, поэтому обновляем только предложение с таким же типом операции
                Optional<Offer> offerWithSameType = existingOffers.stream()
                    .filter(o -> o.getOperationType() == operationType)
                    .max((o1, o2) -> o2.getUpdatedAt().compareTo(o1.getUpdatedAt()));
                
                if (offerWithSameType.isPresent()) {
                    existingOffer = offerWithSameType.get();
                    log.debug("Найдено существующее предложение для обновления: Product={}, Seller={}, OperationType={}, OfferId={}", 
                            product.getModel(), seller.getName(), operationType, existingOffer.getId());
                }
            }
        }
        
        Offer offer;
        boolean isUpdate;
        
        if (existingOffer != null) {
            // Обновляем существующее предложение
            offer = existingOffer;
            offer.setOperationType(operationType); // Обновляем тип операции
            isUpdate = true;
            log.info("Обновление существующего предложения от продавца {} (ID: {}) для модели {} (Offer ID: {}, тип: {})", 
                    seller.getName(), seller.getId(), model, offer.getId(), operationType);
        } else {
            // Создаем новое предложение
            offer = new Offer();
            offer.setProduct(product);
            offer.setSeller(seller); // Устанавливаем связь с продавцом
            offer.setOperationType(operationType); // Устанавливаем тип операции
            isUpdate = false;
            log.info("Создание нового предложения от продавца {} (ID: {}) для модели {} (тип: {})", 
                    seller.getName(), seller.getId(), model, operationType);
        }
        
        // Обновляем/устанавливаем поля предложения
        // Цена - может быть null для запросов на покупку (BUY)
        Object priceObj = productData.get("price");
        if (priceObj != null) {
            try {
                if (priceObj instanceof Number) {
                    offer.setPrice(BigDecimal.valueOf(((Number) priceObj).doubleValue()));
                } else {
                    offer.setPrice(new BigDecimal(priceObj.toString()));
                }
            } catch (Exception e) {
                log.warn("Не удалось распарсить цену: {}", priceObj);
                // Для покупки цена может быть null
                if (operationType != OperationType.BUY && !isUpdate) {
                    offer.setPrice(BigDecimal.ZERO);
                } else if (operationType == OperationType.BUY) {
                    offer.setPrice(null);
                }
            }
        } else {
            // Для запросов на покупку цена может быть null
            if (operationType == OperationType.BUY) {
                offer.setPrice(null);
            } else if (!isUpdate) {
                // Для продажи, если цена не указана и это новое предложение
                offer.setPrice(BigDecimal.ZERO);
            }
        }
        
        // Валюта
        String currency = (String) productData.get("currency");
        if (currency != null || !isUpdate) {
            // Преобразуем "u" в "USD" для совместимости
            if (currency != null && currency.equalsIgnoreCase("u")) {
                currency = "USD";
            }
            offer.setCurrency(currency != null ? currency : "USD");
        }
        
        // Количество
        Object quantityObj = productData.get("quantity");
        if (quantityObj != null) {
            try {
                if (quantityObj instanceof Number) {
                    offer.setQuantity(((Number) quantityObj).intValue());
                } else {
                    offer.setQuantity(Integer.parseInt(quantityObj.toString()));
                }
            } catch (Exception e) {
                log.warn("Не удалось распарсить количество: {}", quantityObj);
                if (!isUpdate) {
                    offer.setQuantity(1);
                }
            }
        } else if (!isUpdate) {
            offer.setQuantity(1);
        }
        
        // Состояние
        String condition = (String) productData.get("condition");
        if (condition != null || !isUpdate) {
            offer.setCondition(condition);
        }
        
        // Дополнительные условия
        String notes = (String) productData.get("notes");
        if (notes == null || notes.isEmpty()) {
            notes = (String) productData.get("additionalConditions");
        }
        if (notes != null || !isUpdate) {
            offer.setNotes(notes);
        }
        
        // Локация
        if (location != null || !isUpdate) {
            offer.setLocation(location);
        }
        
        // Hashrate (мощность майнера)
        String hashrate = (String) productData.get("hashrate");
        if (hashrate != null || !isUpdate) {
            offer.setHashrate(hashrate);
        }
        
        // Manufacturer (производитель)
        String manufacturer = (String) productData.get("manufacturer");
        if (manufacturer != null || !isUpdate) {
            offer.setManufacturer(manufacturer);
        }
        
        // Продавец - устанавливаем связь
        offer.setSeller(seller);
        
        // Обратная совместимость - сохраняем имя и телефон для старых записей
        // Устанавливаем значения из объекта Seller, чтобы deprecated поля были заполнены
        if (seller != null) {
            offer.setSellerName(seller.getName());
            offer.setSellerPhone(seller.getPhone());
        } else {
            // Fallback: если seller не был создан (не должно произойти, но на всякий случай)
            offer.setSellerName("Неизвестный продавец");
            offer.setSellerPhone(null);
        }
        
        // Источник - обновляем на новое сообщение
        offer.setSourceMessageId(messageId);
        if (chatName != null || !isUpdate) {
            offer.setSourceChatName(chatName);
        }
        
        // Сохраняем дополнительные данные из Ollama (все поля, которые не обрабатываются отдельно)
        // Создаем копию productData и удаляем стандартные поля
        try {
            Map<String, Object> additionalDataMap = new HashMap<>(productData);
            // Удаляем стандартные поля, которые уже обработаны
            additionalDataMap.remove("model");
            additionalDataMap.remove("price");
            additionalDataMap.remove("currency");
            additionalDataMap.remove("quantity");
            additionalDataMap.remove("condition");
            additionalDataMap.remove("location");
            additionalDataMap.remove("notes");
            additionalDataMap.remove("additionalConditions");
            additionalDataMap.remove("hashrate");
            additionalDataMap.remove("manufacturer");
            
            // Если есть дополнительные поля, сохраняем их в JSON
            if (!additionalDataMap.isEmpty()) {
                ObjectMapper objectMapper = new ObjectMapper();
                String additionalDataJson = objectMapper.writeValueAsString(additionalDataMap);
                offer.setAdditionalData(additionalDataJson);
                log.info("✅ Сохранены дополнительные данные из Ollama для предложения {}: {}", offer.getId(), additionalDataJson);
            } else {
                offer.setAdditionalData(null);
            }
        } catch (Exception e) {
            log.warn("⚠️  Не удалось сохранить дополнительные данные из Ollama: {}", e.getMessage());
            offer.setAdditionalData(null);
        }
        
        // Сохраняем предложение
        log.info("💾 Попытка сохранения предложения в БД: Product={}, Seller={}, OperationType={}", 
                product.getModel(), seller.getName(), operationType);
        
        Offer savedOffer = offerRepository.save(offer);
        log.info("✅ Предложение успешно сохранено в БД: Offer ID={}", savedOffer.getId());
        
        // Обновляем updatedAt товара, чтобы он всплывал в списке
        product.setUpdatedAt(LocalDateTime.now());
        Product savedProduct = productRepository.save(product);
        log.info("✅ Товар обновлен в БД: Product ID={}, Model={}", savedProduct.getId(), savedProduct.getModel());
        
        if (isUpdate) {
            log.info("🔄 ОБНОВЛЕНО предложение для товара {} от продавца {} (ID: {}): {} {} за {} шт. (Offer ID: {})", 
                    model, seller != null ? seller.getName() : "Unknown", seller != null ? seller.getId() : 0,
                    savedOffer.getPrice(), savedOffer.getCurrency(), savedOffer.getQuantity(), savedOffer.getId());
        } else {
            log.info("➕ СОЗДАНО предложение для товара {} от продавца {} (ID: {}): {} {} за {} шт. (Offer ID: {})", 
                    model, seller != null ? seller.getName() : "Unknown", seller != null ? seller.getId() : 0,
                    savedOffer.getPrice(), savedOffer.getCurrency(), savedOffer.getQuantity(), savedOffer.getId());
        }
        
        return isUpdate;
    }
    
    /**
     * Получает все товары с пагинацией
     */
    @Transactional(readOnly = true)
    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findProductsWithOffers(pageable);
    }
    
    /**
     * Получает товар по ID
     * ВАЖНО: Предложения нужно получать отдельно через getOffersByProductId
     * чтобы избежать LazyInitializationException
     */
    @Transactional(readOnly = true)
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }
    
    /**
     * Получает товар по ID с загрузкой всех предложений и продавцов
     * Используется для страницы детальной информации
     */
    @Transactional(readOnly = true)
    public Optional<Product> getProductWithOffersById(Long id) {
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            // Загружаем предложения через отдельный запрос с JOIN FETCH
            List<Offer> offers = offerRepository.findByProductIdOrderByPriceAsc(id);
            
            // Принудительно инициализируем продавцов для каждого предложения
            offers.forEach(offer -> {
                if (offer.getSeller() != null) {
                    offer.getSeller().getName(); // Инициализируем продавца
                }
            });
            
            // Устанавливаем загруженные предложения в продукт
            product.getOffers().clear();
            product.getOffers().addAll(offers);
        }
        return productOpt;
    }
    
    /**
     * Получает все предложения для товара с загрузкой продавцов
     * Сортировка: сначала продажи (SELL) по цене (от меньшей к большей), потом покупки (BUY) по дате обновления (новые сначала)
     * Использует JOIN FETCH для избежания LazyInitializationException
     */
    @Transactional(readOnly = true)
    public List<Offer> getOffersByProductId(Long productId) {
        // findByProductIdOrderByPriceAsc использует JOIN FETCH для загрузки продавцов
        List<Offer> allOffers = offerRepository.findByProductIdOrderByPriceAsc(productId);
        // Разделяем на продажи и покупки
        List<Offer> sellOffers = allOffers.stream()
                .filter(o -> o.getOperationType() != null && o.getOperationType() == OperationType.SELL)
                .sorted((o1, o2) -> {
                    // Сначала по цене (от меньшей к большей), если цена есть
                    if (o1.getPrice() != null && o2.getPrice() != null) {
                        return o1.getPrice().compareTo(o2.getPrice());
                    }
                    // Если у одного нет цены, он идет в конец
                    if (o1.getPrice() == null) return 1;
                    if (o2.getPrice() == null) return -1;
                    return 0;
                })
                .toList();
        List<Offer> buyOffers = allOffers.stream()
                .filter(o -> o.getOperationType() != null && o.getOperationType() == OperationType.BUY)
                .sorted((o1, o2) -> o2.getUpdatedAt().compareTo(o1.getUpdatedAt())) // Новые сначала
                .toList();
        // Объединяем: сначала продажи, потом покупки
        List<Offer> result = new java.util.ArrayList<>(sellOffers);
        result.addAll(buyOffers);
        return result;
    }
    
    /**
     * Получает предложения для товара с пагинацией и фильтрацией по дате
     * @param productId ID товара
     * @param dateFrom Дата начала периода (может быть null)
     * @param pageable Пагинация и сортировка
     * @return Страница предложений
     */
    @Transactional(readOnly = true)
    public Page<Offer> getOffersByProductIdWithPagination(Long productId, LocalDateTime dateFrom, Pageable pageable) {
        Page<Offer> page;
        if (dateFrom != null) {
            page = offerRepository.findByProductIdAndUpdatedAtGreaterThanEqual(productId, dateFrom, pageable);
        } else {
            page = offerRepository.findByProductIdWithSeller(productId, pageable);
        }
        
        // Принудительно инициализируем продавцов для избежания LazyInitializationException
        page.getContent().forEach(offer -> {
            if (offer.getSeller() != null) {
                offer.getSeller().getName();
            }
            if (offer.getProduct() != null) {
                offer.getProduct().getModel();
            }
        });
        
        return page;
    }
    
    /**
     * Получает все предложения для всех товаров, связанных с MinerDetail
     * @param minerDetailId ID MinerDetail
     * @return Список предложений
     */
    @Transactional(readOnly = true)
    public List<Offer> getOffersByMinerDetailId(Long minerDetailId) {
        // Получаем все товары, связанные с MinerDetail
        List<Product> linkedProducts = productRepository.findByMinerDetailId(minerDetailId);
        
        if (linkedProducts.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        // Собираем все ID связанных товаров
        List<Long> productIds = linkedProducts.stream()
                .map(Product::getId)
                .toList();
        
        // Получаем все предложения для всех связанных товаров
        List<Offer> allOffers = offerRepository.findByProductIdIn(productIds);
        
        // Инициализируем продавцов и товары для избежания LazyInitializationException
        allOffers.forEach(offer -> {
            if (offer.getSeller() != null) {
                offer.getSeller().getName();
            }
            if (offer.getProduct() != null) {
                offer.getProduct().getModel();
            }
        });
        
        return allOffers;
    }
    
    /**
     * Получает предложения для всех товаров, связанных с MinerDetail, с пагинацией и фильтрацией
     * @param minerDetailId ID MinerDetail
     * @param dateFrom Дата начала периода (может быть null)
     * @param operationType Тип операции: SELL или BUY (может быть null)
     * @param hasPrice Только с ценой (true) или все (false, если null)
     * @param pageable Пагинация и сортировка
     * @return Страница предложений
     */
    @Transactional(readOnly = true)
    public Page<Offer> getOffersByMinerDetailIdWithFilters(Long minerDetailId, LocalDateTime dateFrom, OperationType operationType, Boolean hasPrice, Pageable pageable) {
        // Получаем все товары, связанные с MinerDetail
        List<Product> linkedProducts = productRepository.findByMinerDetailId(minerDetailId);
        
        if (linkedProducts.isEmpty()) {
            return new PageImpl<>(new java.util.ArrayList<>(), pageable, 0);
        }
        
        // Собираем все ID связанных товаров
        List<Long> productIds = linkedProducts.stream()
                .map(Product::getId)
                .toList();
        
        // Вычисляем LIMIT и OFFSET из Pageable
        int limitCount = pageable.getPageSize();
        int offsetCount = (int) pageable.getOffset();
        
        // Преобразуем OperationType в строку для SQL запроса (null если не указан)
        String operationTypeStr = operationType != null ? operationType.name() : null;
        
        // Получаем параметры сортировки из Pageable
        String sortBy = "updated_at"; // По умолчанию
        String sortDir = "DESC"; // По умолчанию
        
        if (pageable.getSort().isSorted()) {
            org.springframework.data.domain.Sort.Order order = pageable.getSort().get().findFirst().orElse(null);
            if (order != null) {
                // Преобразуем camelCase в snake_case для SQL
                sortBy = convertCamelCaseToSnakeCase(order.getProperty());
                sortDir = order.getDirection().name();
                
                // Валидация: разрешаем только определенные колонки для безопасности
                if (!isValidSortColumn(sortBy)) {
                    sortBy = "updated_at";
                    log.warn("Недопустимая колонка для сортировки, используем updated_at по умолчанию");
                }
            }
        }
        
        // Валидация направления сортировки
        if (!"ASC".equalsIgnoreCase(sortDir) && !"DESC".equalsIgnoreCase(sortDir)) {
            sortDir = "DESC";
        }
        
        // Строим динамический SQL запрос для данных с поддержкой нескольких product_id
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT * FROM offers WHERE product_id IN (:productIds) ");
        sqlBuilder.append("AND updated_at >= COALESCE(:dateFrom, '1900-01-01'::timestamp) ");
        sqlBuilder.append("AND (CAST(:operationType AS varchar) IS NULL OR operation_type = CAST(:operationType AS varchar)) ");
        // Фильтр "Без пустых цен": если hasPrice = true, показываем только записи с ценой (price IS NOT NULL)
        if (hasPrice != null && hasPrice) {
            sqlBuilder.append("AND price IS NOT NULL ");
        }
        sqlBuilder.append("ORDER BY ").append(sortBy).append(" ").append(sortDir).append(" ");
        sqlBuilder.append("LIMIT :limitCount OFFSET :offsetCount");
        
        // Создаем запрос для получения данных
        Query query = entityManager.createNativeQuery(sqlBuilder.toString(), Offer.class);
        query.setParameter("productIds", productIds);
        query.setParameter("dateFrom", dateFrom);
        query.setParameter("operationType", operationTypeStr);
        query.setParameter("limitCount", limitCount);
        query.setParameter("offsetCount", offsetCount);
        
        @SuppressWarnings("unchecked")
        List<Offer> offers = query.getResultList();
        
        // Получаем общее количество для пагинации
        StringBuilder countSqlBuilder = new StringBuilder();
        countSqlBuilder.append("SELECT COUNT(*) FROM offers WHERE product_id IN (:productIds) ");
        countSqlBuilder.append("AND updated_at >= COALESCE(:dateFrom, '1900-01-01'::timestamp) ");
        countSqlBuilder.append("AND (CAST(:operationType AS varchar) IS NULL OR operation_type = CAST(:operationType AS varchar)) ");
        if (hasPrice != null && hasPrice) {
            countSqlBuilder.append("AND price IS NOT NULL");
        }
        
        Query countQuery = entityManager.createNativeQuery(countSqlBuilder.toString());
        countQuery.setParameter("productIds", productIds);
        countQuery.setParameter("dateFrom", dateFrom);
        countQuery.setParameter("operationType", operationTypeStr);
        
        long totalCount = ((Number) countQuery.getSingleResult()).longValue();
        
        // Принудительно инициализируем продавцов для избежания LazyInitializationException
        offers.forEach(offer -> {
            if (offer.getSeller() != null) {
                offer.getSeller().getName();
            }
            if (offer.getProduct() != null) {
                offer.getProduct().getModel();
            }
        });
        
        // Создаем Page объект вручную
        return new PageImpl<>(offers, pageable, totalCount);
    }
    
    /**
     * Получает предложения для товара с пагинацией и фильтрацией по дате, типу операции и наличию цены
     * @param productId ID товара
     * @param dateFrom Дата начала периода (может быть null)
     * @param operationType Тип операции: SELL или BUY (может быть null)
     * @param hasPrice Только с ценой (true) или все (false, если null)
     * @param pageable Пагинация и сортировка
     * @return Страница предложений
     */
    @Transactional(readOnly = true)
    public Page<Offer> getOffersByProductIdWithFilters(Long productId, LocalDateTime dateFrom, OperationType operationType, Boolean hasPrice, Pageable pageable) {
        // Вычисляем LIMIT и OFFSET из Pageable
        int limitCount = pageable.getPageSize();
        int offsetCount = (int) pageable.getOffset();
        
        // Преобразуем OperationType в строку для SQL запроса (null если не указан)
        String operationTypeStr = operationType != null ? operationType.name() : null;
        
        // Получаем параметры сортировки из Pageable
        String sortBy = "updated_at"; // По умолчанию
        String sortDir = "DESC"; // По умолчанию
        
        if (pageable.getSort().isSorted()) {
            org.springframework.data.domain.Sort.Order order = pageable.getSort().get().findFirst().orElse(null);
            if (order != null) {
                // Преобразуем camelCase в snake_case для SQL
                sortBy = convertCamelCaseToSnakeCase(order.getProperty());
                sortDir = order.getDirection().name();
                
                // Валидация: разрешаем только определенные колонки для безопасности
                if (!isValidSortColumn(sortBy)) {
                    sortBy = "updated_at";
                    log.warn("Недопустимая колонка для сортировки, используем updated_at по умолчанию");
                }
            }
        }
        
        // Валидация направления сортировки
        if (!"ASC".equalsIgnoreCase(sortDir) && !"DESC".equalsIgnoreCase(sortDir)) {
            sortDir = "DESC";
        }
        
        // Строим динамический SQL запрос для данных
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT * FROM offers WHERE product_id = :productId ");
        sqlBuilder.append("AND updated_at >= COALESCE(:dateFrom, '1900-01-01'::timestamp) ");
        sqlBuilder.append("AND (CAST(:operationType AS varchar) IS NULL OR operation_type = CAST(:operationType AS varchar)) ");
        // Фильтр "Без пустых цен": если hasPrice = true, показываем только записи с ценой (price IS NOT NULL)
        if (hasPrice != null && hasPrice) {
            sqlBuilder.append("AND price IS NOT NULL ");
        }
        sqlBuilder.append("ORDER BY ").append(sortBy).append(" ").append(sortDir).append(" ");
        sqlBuilder.append("LIMIT :limitCount OFFSET :offsetCount");
        
        // Создаем запрос для получения данных
        Query query = entityManager.createNativeQuery(sqlBuilder.toString(), Offer.class);
        query.setParameter("productId", productId);
        query.setParameter("dateFrom", dateFrom);
        query.setParameter("operationType", operationTypeStr);
        query.setParameter("limitCount", limitCount);
        query.setParameter("offsetCount", offsetCount);
        
        @SuppressWarnings("unchecked")
        List<Offer> offers = query.getResultList();
        
        // Получаем общее количество для пагинации (без фильтра по цене, так как он применяется динамически)
        long totalCount = offerRepository.countByProductIdWithFilters(productId, dateFrom, operationTypeStr);
        
        // Если фильтр "Без пустых цен" активен, нужно дополнительно отфильтровать результаты подсчета
        if (hasPrice != null && hasPrice) {
            // Строим запрос для подсчета с фильтром по цене
            StringBuilder countSqlBuilder = new StringBuilder();
            countSqlBuilder.append("SELECT COUNT(*) FROM offers WHERE product_id = :productId ");
            countSqlBuilder.append("AND updated_at >= COALESCE(:dateFrom, '1900-01-01'::timestamp) ");
            countSqlBuilder.append("AND (CAST(:operationType AS varchar) IS NULL OR operation_type = CAST(:operationType AS varchar)) ");
            countSqlBuilder.append("AND price IS NOT NULL");
            
            Query countQuery = entityManager.createNativeQuery(countSqlBuilder.toString());
            countQuery.setParameter("productId", productId);
            countQuery.setParameter("dateFrom", dateFrom);
            countQuery.setParameter("operationType", operationTypeStr);
            
            totalCount = ((Number) countQuery.getSingleResult()).longValue();
        }
        
        // Принудительно инициализируем продавцов для избежания LazyInitializationException
        offers.forEach(offer -> {
            if (offer.getSeller() != null) {
                offer.getSeller().getName();
            }
            if (offer.getProduct() != null) {
                offer.getProduct().getModel();
            }
        });
        
        // Создаем Page объект вручную
        return new PageImpl<>(offers, pageable, totalCount);
    }
    
    /**
     * Преобразует camelCase в snake_case для использования в SQL запросах
     */
    private String convertCamelCaseToSnakeCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        // Маппинг полей entity в колонки БД
        return switch (camelCase) {
            case "updatedAt" -> "updated_at";
            case "createdAt" -> "created_at";
            case "operationType" -> "operation_type";
            case "price" -> "price";
            case "quantity" -> "quantity";
            case "hashrate" -> "hashrate";
            case "condition" -> "condition";
            case "location" -> "location";
            case "sellerName" -> "seller_name";
            case "sellerPhone" -> "seller_phone";
            case "sourceMessageId" -> "source_message_id";
            case "sourceChatName" -> "source_chat_name";
            case "additionalData" -> "additional_data";
            default -> "updated_at"; // По умолчанию, если не найдено
        };
    }
    
    /**
     * Проверяет, является ли колонка допустимой для сортировки (защита от SQL инъекций)
     */
    private boolean isValidSortColumn(String column) {
        // Разрешенные колонки для сортировки (snake_case)
        return List.of("updated_at", "created_at", "operation_type", "price", "quantity", 
                       "hashrate", "condition", "location", "seller_name", "seller_phone").contains(column);
    }
    
    /**
     * Получает минимальную цену для товара из предложений на продажу
     */
    @Transactional(readOnly = true)
    public java.math.BigDecimal getMinPriceForProduct(Long productId) {
        List<Offer> sellOffers = offerRepository.findByProductIdOrderByPriceAsc(productId).stream()
                .filter(o -> o.getOperationType() != null && o.getOperationType() == OperationType.SELL && o.getPrice() != null)
                .toList();
        
        if (sellOffers.isEmpty()) {
            return null;
        }
        
        return sellOffers.stream()
                .map(Offer::getPrice)
                .filter(price -> price != null)
                .min(java.math.BigDecimal::compareTo)
                .orElse(null);
    }
    
    /**
     * Получает статистику
     */
    @Transactional(readOnly = true)
    public long getTotalProducts() {
        return productRepository.count();
    }
    
    @Transactional(readOnly = true)
    public long getTotalOffers() {
        return offerRepository.count();
    }
}

