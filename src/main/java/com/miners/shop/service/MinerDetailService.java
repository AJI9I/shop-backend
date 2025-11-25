package com.miners.shop.service;

import com.miners.shop.entity.MinerDetail;
import com.miners.shop.entity.Offer;
import com.miners.shop.entity.Product;
import com.miners.shop.repository.MinerDetailRepository;
import com.miners.shop.repository.OfferRepository;
import com.miners.shop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Сервис для работы с детальной информацией о майнерах
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MinerDetailService {
    
    private final MinerDetailRepository minerDetailRepository;
    private final ProductRepository productRepository;
    private final OfferRepository offerRepository;
    
    /**
     * Создает детальную запись для товара с данными из нейросети
     * Вызывается автоматически при создании нового Product
     * 
     * @param product Товар, для которого создается детальная запись
     * @return Созданная детальная запись
     */
    @Transactional
    @CacheEvict(value = "minerDetails", allEntries = true)
    public MinerDetail createMinerDetailForProduct(Product product) {
        log.info("Создание детальной записи для товара: {} (ID: {})", product.getModel(), product.getId());
        
        MinerDetail minerDetail = new MinerDetail();
        
        // Заполняем стандартизированное название из model товара
        minerDetail.setStandardName(product.getModel());
        
        // Заполняем производителя, если он есть в товаре
        if (product.getManufacturer() != null && !product.getManufacturer().trim().isEmpty()) {
            minerDetail.setManufacturer(product.getManufacturer().trim());
        }
        
        // Пытаемся извлечь серию из названия модели
        // Например: "S19j PRO 104T" -> серия "S19j"
        String series = extractSeriesFromModel(product.getModel());
        if (series != null && !series.isEmpty()) {
            minerDetail.setSeries(series);
            log.debug("Извлечена серия из модели: {} -> серия: {}", product.getModel(), series);
        }
        
        // Остальные поля остаются пустыми и будут заполнены администратором или ИИ
        // Сохраняем детальную запись
        MinerDetail saved = minerDetailRepository.save(minerDetail);
        log.info("Создана детальная запись для товара {}: ID={}, стандартное название={}", 
                product.getModel(), saved.getId(), saved.getStandardName());
        
        // Устанавливаем связь в товаре
        product.setMinerDetail(saved);
        productRepository.save(product);
        
        return saved;
    }
    
    /**
     * Извлекает серию из названия модели
     * Логика: ищет паттерн вида "S19j", "L7", "S21" и т.д.
     * 
     * @param model Название модели
     * @return Серия или null
     */
    private String extractSeriesFromModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            return null;
        }
        
        // Удаляем лишние пробелы
        String trimmed = model.trim();
        
        // Паттерны для поиска серии:
        // - "S19j PRO 104T" -> "S19j"
        // - "L7 9050M" -> "L7"
        // - "S21 200T" -> "S21"
        
        // Ищем паттерн: буква + цифры + опционально буква
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^([A-Z]\\d+[a-z]?|[A-Z]\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(trimmed);
        
        if (matcher.find()) {
            String series = matcher.group(1);
            log.debug("Извлечена серия '{}' из модели '{}'", series, model);
            return series;
        }
        
        return null;
    }
    
    /**
     * Получает детальную запись по ID
     */
    @Transactional(readOnly = true)
    public Optional<MinerDetail> getMinerDetailById(Long id) {
        return minerDetailRepository.findById(id);
    }
    
    /**
     * Обновляет детальную запись
     */
    @Transactional
    @CacheEvict(value = "minerDetails", allEntries = true)
    public MinerDetail updateMinerDetail(MinerDetail minerDetail) {
        log.info("Обновление детальной записи: ID={}, стандартное название={}", 
                minerDetail.getId(), minerDetail.getStandardName());
        return minerDetailRepository.save(minerDetail);
    }
    
    /**
     * Объединяет несколько товаров, связав их с одной целевой детальной записью
     * 
     * Логика объединения:
     * 1. Выбирается целевая MinerDetail (в которую объединяются товары)
     * 2. Выбираются один или несколько Product (которые объединяются)
     * 3. У всех выбранных Product устанавливается minerDetailId = targetMinerDetailId
     * 
     * Пример:
     * - MinerDetail ID=1 (целевая, в которую объединяем)
     * - Product ID=2, ID=3, ID=5 (объединяемые товары)
     * - Результат: у Product ID=2, ID=3, ID=5 устанавливается minerDetailId = 1
     * 
     * @param targetMinerDetailId ID целевой детальной записи (в которую объединяются товары)
     * @param productIds Список ID товаров, которые объединяются в целевую детальную запись
     */
    @Transactional
    public void mergeProducts(Long targetMinerDetailId, List<Long> productIds) {
        log.info("Объединение товаров: {} товаров будут привязаны к целевой MinerDetail ID={}", 
                productIds.size(), targetMinerDetailId);
        
        // Проверяем, что целевая MinerDetail существует
        Optional<MinerDetail> targetMinerDetailOpt = minerDetailRepository.findById(targetMinerDetailId);
        if (targetMinerDetailOpt.isEmpty()) {
            throw new IllegalArgumentException("Целевая MinerDetail с ID=" + targetMinerDetailId + " не найдена");
        }
        
        MinerDetail targetMinerDetail = targetMinerDetailOpt.get();
        log.info("Целевая MinerDetail: ID={}, стандартное название={}", 
                targetMinerDetail.getId(), targetMinerDetail.getStandardName());
        
        // Устанавливаем выбранную целевую MinerDetail всем объединяемым товарам
        for (Long productId : productIds) {
            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                
                // Получаем текущую MinerDetail (если есть) для логирования
                MinerDetail previousMinerDetail = product.getMinerDetail();
                String previousInfo = previousMinerDetail != null 
                        ? "ID=" + previousMinerDetail.getId() + " (" + previousMinerDetail.getStandardName() + ")"
                        : "нет";
                
                // Устанавливаем целевую MinerDetail
                product.setMinerDetail(targetMinerDetail);
                productRepository.save(product);
                
                log.info("Товар ID={} ({}) объединен: {} -> MinerDetail ID={} ({})", 
                        productId, 
                        product.getModel(),
                        previousInfo,
                        targetMinerDetail.getId(),
                        targetMinerDetail.getStandardName());
            } else {
                log.warn("Товар с ID={} не найден, пропускаем", productId);
            }
        }
        
        log.info("Объединение товаров завершено: {} товаров привязаны к целевой MinerDetail ID={} ({})", 
                productIds.size(), 
                targetMinerDetailId,
                targetMinerDetail.getStandardName());
    }
    
    /**
     * Получает все детальные записи
     */
    @Transactional(readOnly = true)
    public List<MinerDetail> getAllMinerDetails() {
        return minerDetailRepository.findAll();
    }
    
    /**
     * Получает MinerDetail, отсортированные по последнему обновлению предложений, с пагинацией
     * @param pageable Пагинация
     * @param manufacturers Список производителей для фильтрации (null или пустой = все)
     * @param series Список серий для фильтрации (null или пустой = все)
     * @return Страница MinerDetail, отсортированная по MAX(offer.updatedAt)
     */
    @Transactional(readOnly = true)
    public Page<MinerDetail> findAllSortedByLatestOfferUpdate(Pageable pageable, List<String> manufacturers, List<String> series) {
        // Получаем все MinerDetail, у которых есть предложения, с фильтрацией
        List<MinerDetail> allMinerDetailsWithOffers;
        
        // Нормализуем параметры фильтров: если список пустой, передаем null
        List<String> normalizedManufacturers = (manufacturers != null && !manufacturers.isEmpty()) ? manufacturers : null;
        List<String> normalizedSeries = (series != null && !series.isEmpty()) ? series : null;
        
        if (normalizedManufacturers == null && normalizedSeries == null) {
            // Без фильтров - получаем все
            allMinerDetailsWithOffers = minerDetailRepository.findAllWithOffers();
        } else if (normalizedManufacturers != null && normalizedSeries != null) {
            // Оба фильтра
            allMinerDetailsWithOffers = minerDetailRepository.findAllWithOffersByManufacturersAndSeries(normalizedManufacturers, normalizedSeries);
        } else if (normalizedManufacturers != null) {
            // Только производитель
            allMinerDetailsWithOffers = minerDetailRepository.findAllWithOffersByManufacturers(normalizedManufacturers);
        } else {
            // Только серия
            allMinerDetailsWithOffers = minerDetailRepository.findAllWithOffersBySeries(normalizedSeries);
        }
        
        if (allMinerDetailsWithOffers.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        
        // Получаем все Product ID для этих MinerDetail
        List<Long> minerDetailIds = allMinerDetailsWithOffers.stream()
                .map(MinerDetail::getId)
                .collect(Collectors.toList());
        
        // Получаем все Product, связанные с этими MinerDetail
        List<Product> allProducts = minerDetailIds.stream()
                .flatMap(id -> productRepository.findByMinerDetailId(id).stream())
                .collect(Collectors.toList());
        
        List<Long> productIds = allProducts.stream()
                .map(Product::getId)
                .collect(Collectors.toList());
        
        // Получаем все offers для этих продуктов
        List<Offer> allOffers = offerRepository.findByProductIdIn(productIds);
        
        // Группируем offers по MinerDetail ID
        Map<Long, List<Offer>> offersByMinerDetailId = new HashMap<>();
        for (Product product : allProducts) {
            if (product.getMinerDetail() != null) {
                Long minerDetailId = product.getMinerDetail().getId();
                List<Offer> productOffers = allOffers.stream()
                        .filter(o -> o.getProduct().getId().equals(product.getId()))
                        .collect(Collectors.toList());
                
                offersByMinerDetailId.computeIfAbsent(minerDetailId, k -> new java.util.ArrayList<>())
                        .addAll(productOffers);
            }
        }
        
        // Вычисляем максимальную дату обновления для каждого MinerDetail
        Map<Long, LocalDateTime> maxUpdateDateByMinerDetailId = new HashMap<>();
        for (Map.Entry<Long, List<Offer>> entry : offersByMinerDetailId.entrySet()) {
            Optional<LocalDateTime> maxDate = entry.getValue().stream()
                    .map(Offer::getUpdatedAt)
                    .filter(date -> date != null)
                    .max(LocalDateTime::compareTo);
            
            maxUpdateDateByMinerDetailId.put(entry.getKey(), maxDate.orElse(null));
        }
        
        // Логируем текущую дату для отладки
        LocalDateTime now = LocalDateTime.now();
        log.info("Текущая дата и время: {}", now);
        log.info("Всего майнеров с предложениями: {}", allMinerDetailsWithOffers.size());
        
        // Логируем даты обновления для первых 10 майнеров (до сортировки)
        log.info("Даты последнего обновления предложений (до сортировки):");
        for (int i = 0; i < Math.min(10, allMinerDetailsWithOffers.size()); i++) {
            MinerDetail md = allMinerDetailsWithOffers.get(i);
            LocalDateTime maxOfferDate = maxUpdateDateByMinerDetailId.get(md.getId());
            if (maxOfferDate != null) {
                log.info("  MinerDetail ID={}, название={}, макс. дата предложения={}", 
                        md.getId(), md.getStandardName(), maxOfferDate);
            }
        }
        
        // Сортируем MinerDetail по максимальной дате обновления предложений (DESC - самые свежие первыми)
        // Используем явную сортировку по убыванию даты
        List<MinerDetail> sorted = allMinerDetailsWithOffers.stream()
                .sorted((md1, md2) -> {
                    // Получаем максимальные даты обновления предложений для обоих майнеров
                    LocalDateTime maxDate1 = maxUpdateDateByMinerDetailId.get(md1.getId());
                    LocalDateTime maxDate2 = maxUpdateDateByMinerDetailId.get(md2.getId());
                    
                    // Если у первого майнера нет даты - он идет в конец
                    if (maxDate1 == null && maxDate2 == null) {
                        // Если у обоих нет даты, сортируем по updatedAt MinerDetail
                        LocalDateTime upd1 = md1.getUpdatedAt() != null ? md1.getUpdatedAt() : LocalDateTime.MIN;
                        LocalDateTime upd2 = md2.getUpdatedAt() != null ? md2.getUpdatedAt() : LocalDateTime.MIN;
                        return upd2.compareTo(upd1); // DESC
                    }
                    if (maxDate1 == null) {
                        return 1; // Первый майнер идет в конец
                    }
                    if (maxDate2 == null) {
                        return -1; // Второй майнер идет в конец
                    }
                    
                    // Сравниваем даты обновления предложений (DESC - новые первыми)
                    int compareResult = maxDate2.compareTo(maxDate1); // maxDate2.compareTo(maxDate1) дает DESC
                    
                    // Если даты предложений равны, сортируем по updatedAt MinerDetail
                    if (compareResult == 0) {
                        LocalDateTime upd1 = md1.getUpdatedAt() != null ? md1.getUpdatedAt() : LocalDateTime.MIN;
                        LocalDateTime upd2 = md2.getUpdatedAt() != null ? md2.getUpdatedAt() : LocalDateTime.MIN;
                        return upd2.compareTo(upd1); // DESC
                    }
                    
                    return compareResult;
                })
                .collect(Collectors.toList());
        
        // Логируем первые 10 майнеров после сортировки для отладки
        log.info("Результат сортировки майнеров (первые 10):");
        for (int i = 0; i < Math.min(10, sorted.size()); i++) {
            MinerDetail md = sorted.get(i);
            LocalDateTime maxOfferDate = maxUpdateDateByMinerDetailId.get(md.getId());
            log.info("  {}: MinerDetail ID={}, название={}, макс. дата предложения={}", 
                    i + 1, md.getId(), md.getStandardName(), 
                    maxOfferDate != null ? maxOfferDate : "нет предложений");
        }
        
        // Применяем пагинацию
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sorted.size());
        
        if (start >= sorted.size() || sorted.isEmpty()) {
            // Если запрошенная страница вне диапазона или список пуст, возвращаем пустую страницу
            return new PageImpl<>(List.of(), pageable, sorted.size());
        }
        
        // Убеждаемся, что start не отрицательный
        if (start < 0) {
            start = 0;
        }
        
        // Убеждаемся, что end не меньше start
        if (end <= start) {
            end = Math.min(start + pageable.getPageSize(), sorted.size());
        }
        
        List<MinerDetail> pageContent = sorted.subList(start, end);
        return new PageImpl<>(pageContent, pageable, sorted.size());
    }
    
    /**
     * Сортирует список MinerDetail по последнему обновлению предложений
     * Принимает уже отфильтрованный список и сортирует его
     * @param minerDetails Список MinerDetail для сортировки
     * @return Отсортированный список MinerDetail
     */
    @Transactional(readOnly = true)
    public List<MinerDetail> sortByLatestOfferUpdate(List<MinerDetail> minerDetails) {
        if (minerDetails == null || minerDetails.isEmpty()) {
            return minerDetails != null ? minerDetails : List.of();
        }
        
        // Получаем все Product, связанные с этими MinerDetail
        List<Long> minerDetailIds = minerDetails.stream()
                .map(MinerDetail::getId)
                .collect(Collectors.toList());
        
        List<Product> allProducts = minerDetailIds.stream()
                .flatMap(id -> productRepository.findByMinerDetailId(id).stream())
                .collect(Collectors.toList());
        
        List<Long> productIds = allProducts.stream()
                .map(Product::getId)
                .collect(Collectors.toList());
        
        // Получаем все offers для этих продуктов
        List<Offer> allOffers = offerRepository.findByProductIdIn(productIds);
        
        // Группируем offers по MinerDetail ID
        Map<Long, List<Offer>> offersByMinerDetailId = new HashMap<>();
        for (Product product : allProducts) {
            if (product.getMinerDetail() != null) {
                Long minerDetailId = product.getMinerDetail().getId();
                List<Offer> productOffers = allOffers.stream()
                        .filter(o -> o.getProduct().getId().equals(product.getId()))
                        .collect(Collectors.toList());
                
                offersByMinerDetailId.computeIfAbsent(minerDetailId, k -> new java.util.ArrayList<>())
                        .addAll(productOffers);
            }
        }
        
        // Вычисляем максимальную дату обновления для каждого MinerDetail
        Map<Long, LocalDateTime> maxUpdateDateByMinerDetailId = new HashMap<>();
        for (Map.Entry<Long, List<Offer>> entry : offersByMinerDetailId.entrySet()) {
            Optional<LocalDateTime> maxDate = entry.getValue().stream()
                    .map(Offer::getUpdatedAt)
                    .filter(date -> date != null)
                    .max(LocalDateTime::compareTo);
            
            maxUpdateDateByMinerDetailId.put(entry.getKey(), maxDate.orElse(null));
        }
        
        // Сортируем MinerDetail по максимальной дате обновления предложений (DESC - самые свежие первыми)
        return minerDetails.stream()
                .sorted((md1, md2) -> {
                    LocalDateTime maxDate1 = maxUpdateDateByMinerDetailId.get(md1.getId());
                    LocalDateTime maxDate2 = maxUpdateDateByMinerDetailId.get(md2.getId());
                    
                    if (maxDate1 == null && maxDate2 == null) {
                        LocalDateTime upd1 = md1.getUpdatedAt() != null ? md1.getUpdatedAt() : LocalDateTime.MIN;
                        LocalDateTime upd2 = md2.getUpdatedAt() != null ? md2.getUpdatedAt() : LocalDateTime.MIN;
                        return upd2.compareTo(upd1); // DESC
                    }
                    if (maxDate1 == null) {
                        return 1;
                    }
                    if (maxDate2 == null) {
                        return -1;
                    }
                    
                    int compareResult = maxDate2.compareTo(maxDate1); // DESC
                    
                    if (compareResult == 0) {
                        LocalDateTime upd1 = md1.getUpdatedAt() != null ? md1.getUpdatedAt() : LocalDateTime.MIN;
                        LocalDateTime upd2 = md2.getUpdatedAt() != null ? md2.getUpdatedAt() : LocalDateTime.MIN;
                        return upd2.compareTo(upd1); // DESC
                    }
                    
                    return compareResult;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Получает список уникальных производителей
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "minerDetails", key = "'manufacturers'")
    public List<String> getDistinctManufacturers() {
        return minerDetailRepository.findDistinctManufacturers();
    }
    
    /**
     * Получает список уникальных серий
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "minerDetails", key = "'series'")
    public List<String> getDistinctSeries() {
        return minerDetailRepository.findDistinctSeries();
    }
    
    /**
     * Получает список уникальных серий для выбранных производителей
     */
    @Transactional(readOnly = true)
    public List<String> getDistinctSeriesByManufacturers(List<String> manufacturers) {
        if (manufacturers == null || manufacturers.isEmpty()) {
            return getDistinctSeries();
        }
        return minerDetailRepository.findDistinctSeriesByManufacturers(manufacturers);
    }
    
    /**
     * Получает список уникальных алгоритмов
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "minerDetails", key = "'algorithms'")
    public List<String> getDistinctAlgorithms() {
        return minerDetailRepository.findDistinctAlgorithms();
    }
    
    /**
     * Инициализация: создает MinerDetail для всех существующих Product, у которых еще нет MinerDetail
     * Используется для миграции существующих данных
     * 
     * @return Количество созданных MinerDetail записей
     */
    @Transactional
    public int initializeMinerDetailsForExistingProducts() {
        log.info("Начало инициализации MinerDetail для существующих товаров");
        
        // Получаем все товары, у которых нет MinerDetail
        List<Product> productsWithoutDetail = productRepository.findAll().stream()
                .filter(product -> product.getMinerDetail() == null)
                .collect(java.util.stream.Collectors.toList());
        
        log.info("Найдено товаров без MinerDetail: {}", productsWithoutDetail.size());
        
        int createdCount = 0;
        int errorCount = 0;
        
        for (Product product : productsWithoutDetail) {
            try {
                // Создаем MinerDetail для товара
                MinerDetail minerDetail = createMinerDetailForProduct(product);
                product.setMinerDetail(minerDetail);
                productRepository.save(product);
                createdCount++;
                log.debug("Создан MinerDetail для товара {} (ID: {}) -> MinerDetail ID={}", 
                        product.getModel(), product.getId(), minerDetail.getId());
            } catch (Exception e) {
                errorCount++;
                log.error("Ошибка при создании MinerDetail для товара {} (ID: {}): {}", 
                        product.getModel(), product.getId(), e.getMessage(), e);
            }
        }
        
        log.info("Инициализация завершена: создано {}, ошибок: {}", createdCount, errorCount);
        return createdCount;
    }
    
    /**
     * Полная инициализация MinerDetail с учетом групп объединения
     * 
     * Шаги:
     * 1. Исправляет ошибки в manufacturer для товаров
     * 2. Создает MinerDetail для групп объединения (12 групп)
     * 3. Создает MinerDetail для уникальных майнеров (~35 товаров)
     * 4. Связывает все Product с соответствующими MinerDetail
     * 
     * @return Количество созданных MinerDetail записей
     */
    @Transactional
    public int initializeMinersDetailsWithGroups() {
        log.info("========================================");
        log.info("Начало полной инициализации MinerDetail с группами");
        log.info("========================================");
        
        int totalCreated = 0;
        int fixedManufacturers = 0;
        
        // Шаг 1: Исправление ошибок в manufacturer
        log.info("\n[ШАГ 1] Исправление ошибок в manufacturer...");
        fixedManufacturers = fixManufacturerErrors();
        log.info("Исправлено производителей: {}", fixedManufacturers);
        
        // Шаг 2: Создание MinerDetail для групп объединения
        log.info("\n[ШАГ 2] Создание MinerDetail для групп объединения...");
        int groupsCreated = createMinerDetailsForGroups();
        totalCreated += groupsCreated;
        log.info("Создано MinerDetail для групп: {}", groupsCreated);
        
        // Шаг 3: Создание MinerDetail для уникальных майнеров
        log.info("\n[ШАГ 3] Создание MinerDetail для уникальных майнеров...");
        int uniqueCreated = createMinerDetailsForUniqueMiners();
        totalCreated += uniqueCreated;
        log.info("Создано MinerDetail для уникальных майнеров: {}", uniqueCreated);
        
        log.info("\n========================================");
        log.info("Инициализация завершена:");
        log.info("- Исправлено производителей: {}", fixedManufacturers);
        log.info("- Всего создано MinerDetail: {}", totalCreated);
        log.info("========================================");
        
        return totalCreated;
    }
    
    /**
     * Исправляет ошибки в manufacturer для товаров
     * @return Количество исправленных товаров
     */
    @Transactional
    public int fixManufacturerErrors() {
        // Маппинг: Product ID -> правильный manufacturer
        java.util.Map<Long, String> corrections = new java.util.HashMap<>();
        corrections.put(5L, "Bitmain");    // s19 pro+
        corrections.put(6L, "MicroBT");    // z15 pro
        corrections.put(37L, "MicroBT");   // jpro 104
        corrections.put(67L, "MicroBT");   // М33S++
        corrections.put(74L, "Canaan");    // Avalon
        corrections.put(75L, "MicroBT");   // Z15 PRO
        corrections.put(76L, "Bitmain");   // S19J PRO+
        corrections.put(77L, "MicroBT");   // Z15Pro
        corrections.put(80L, "MicroBT");   // Z15 Pro
        corrections.put(81L, "MicroBT");   // M33s+
        corrections.put(82L, "MicroBT");   // M60s+
        corrections.put(84L, "MicroBT");   // М50
        corrections.put(88L, "MicroBT");   // Z15 pro
        corrections.put(27L, "ElphapeX");  // DG1+
        corrections.put(29L, "ElphapeX");  // Dg1
        corrections.put(30L, "ElphapeX");  // Dg1+
        
        int fixed = 0;
        for (java.util.Map.Entry<Long, String> entry : corrections.entrySet()) {
            Optional<Product> productOpt = productRepository.findById(entry.getKey());
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                String oldManufacturer = product.getManufacturer();
                product.setManufacturer(entry.getValue());
                productRepository.save(product);
                fixed++;
                log.info("Исправлен manufacturer для Product ID={} ({}): '{}' -> '{}'", 
                        entry.getKey(), product.getModel(), oldManufacturer, entry.getValue());
            }
        }
        return fixed;
    }
    
    /**
     * Создает MinerDetail для групп объединения и связывает товары
     * @return Количество созданных MinerDetail
     */
    @Transactional
    public int createMinerDetailsForGroups() {
        // Группы для объединения
        java.util.List<GroupInfo> groups = new java.util.ArrayList<>();
        
        // Группа 1: S19j PRO
        groups.add(new GroupInfo("Antminer S19j PRO", "Bitmain", "S19j", 
                java.util.Arrays.asList(16L, 32L, 39L, 42L, 44L, 76L)));
        
        // Группа 2: S19k PRO
        groups.add(new GroupInfo("Antminer S19k PRO", "Bitmain", "S19k", 
                java.util.Arrays.asList(2L, 20L, 22L, 38L, 41L, 43L, 64L)));
        
        // Группа 3: S19 PRO
        groups.add(new GroupInfo("Antminer S19 PRO", "Bitmain", "S19", 
                java.util.Arrays.asList(5L, 15L, 78L)));
        
        // Группа 4: M30S++
        groups.add(new GroupInfo("Whatsminer M30S++", "MicroBT", "M30S", 
                java.util.Arrays.asList(4L, 8L, 35L, 47L, 48L)));
        
        // Группа 5: M30S+
        groups.add(new GroupInfo("Whatsminer M30S+", "MicroBT", "M30S", 
                java.util.Arrays.asList(3L, 33L, 65L)));
        
        // Группа 6: M30S
        groups.add(new GroupInfo("Whatsminer M30S", "MicroBT", "M30S", 
                java.util.Arrays.asList(49L, 66L, 87L)));
        
        // Группа 7: M50
        groups.add(new GroupInfo("Whatsminer M50S++", "MicroBT", "M50", 
                java.util.Arrays.asList(11L, 12L, 34L, 46L, 51L, 52L, 84L)));
        
        // Группа 8: Z15 PRO
        groups.add(new GroupInfo("Whatsminer Z15 PRO", "MicroBT", "Z15", 
                java.util.Arrays.asList(6L, 31L, 75L, 77L, 80L, 88L)));
        
        // Группа 9: DG1
        groups.add(new GroupInfo("ElphapeX DG1+", "ElphapeX", "DG1", 
                java.util.Arrays.asList(27L, 29L, 30L, 61L)));
        
        // Группа 10: S19 XP
        groups.add(new GroupInfo("Antminer S19 XP", "Bitmain", "S19", 
                java.util.Arrays.asList(13L, 40L)));
        
        // Группа 11: S21
        groups.add(new GroupInfo("Antminer S21", "Bitmain", "S21", 
                java.util.Arrays.asList(18L, 23L, 24L, 90L)));
        
        // Группа 12: JPRO
        groups.add(new GroupInfo("Whatsminer JPRO", "MicroBT", "JPRO", 
                java.util.Arrays.asList(36L, 37L)));
        
        int created = 0;
        for (GroupInfo group : groups) {
            try {
                // Создаем MinerDetail для группы
                MinerDetail minerDetail = new MinerDetail();
                minerDetail.setStandardName(group.standardName);
                minerDetail.setManufacturer(group.manufacturer);
                minerDetail.setSeries(group.series);
                
                MinerDetail saved = minerDetailRepository.save(minerDetail);
                log.info("✅ Создан MinerDetail ID={} для группы: {}", saved.getId(), group.standardName);
                
                // Связываем все товары группы с этим MinerDetail
                for (Long productId : group.productIds) {
                    Optional<Product> productOpt = productRepository.findById(productId);
                    if (productOpt.isPresent()) {
                        Product product = productOpt.get();
                        product.setMinerDetail(saved);
                        productRepository.save(product);
                        log.debug("  → Связан Product ID={} ({})", productId, product.getModel());
                    }
                }
                
                created++;
            } catch (Exception e) {
                log.error("❌ Ошибка при создании MinerDetail для группы {}: {}", 
                        group.standardName, e.getMessage(), e);
            }
        }
        
        return created;
    }
    
    /**
     * Создает MinerDetail для уникальных майнеров (не входящих в группы)
     * @return Количество созданных MinerDetail
     */
    @Transactional
    public int createMinerDetailsForUniqueMiners() {
        // Уникальные майнеры (Product ID -> стандартное название, производитель, серия)
        java.util.List<MinerInfo> uniqueMiners = new java.util.ArrayList<>();
        
        uniqueMiners.add(new MinerInfo(1L, "Antminer T21", "Bitmain", "T21"));
        uniqueMiners.add(new MinerInfo(7L, "Antminer S19", "Bitmain", "S19"));
        uniqueMiners.add(new MinerInfo(9L, "Antminer S19j", "Bitmain", "S19j"));
        uniqueMiners.add(new MinerInfo(10L, "AvalonMiner 1246", "Canaan", "AvalonMiner"));
        uniqueMiners.add(new MinerInfo(14L, "Antminer S19a", "Bitmain", "S19"));
        uniqueMiners.add(new MinerInfo(17L, "Antminer S19 90/126", "Bitmain", "S19"));
        uniqueMiners.add(new MinerInfo(19L, "Antminer L7", "Bitmain", "L7"));
        uniqueMiners.add(new MinerInfo(21L, "Antminer S19/S19j PRO", "Bitmain", "S19"));
        uniqueMiners.add(new MinerInfo(25L, "Antminer S21 XP", "Bitmain", "S21"));
        uniqueMiners.add(new MinerInfo(26L, "Whatsminer L1 PRO", "MicroBT", "L1"));
        uniqueMiners.add(new MinerInfo(28L, "Antminer L9", "Bitmain", "L9"));
        uniqueMiners.add(new MinerInfo(45L, "Antminer E9", "Bitmain", "E9"));
        uniqueMiners.add(new MinerInfo(50L, "Antminer S21 XP", "Bitmain", "S21"));
        uniqueMiners.add(new MinerInfo(53L, "Whatsminer M60", "MicroBT", "M60"));
        uniqueMiners.add(new MinerInfo(54L, "Whatsminer M61", "MicroBT", "M61"));
        uniqueMiners.add(new MinerInfo(55L, "Whatsminer M63S+", "MicroBT", "M63"));
        uniqueMiners.add(new MinerInfo(56L, "Whatsminer M63S", "MicroBT", "M63"));
        uniqueMiners.add(new MinerInfo(57L, "Antminer KS5P", "Bitmain", "KS5"));
        uniqueMiners.add(new MinerInfo(58L, "Jasminer X16-Q", "Jasminer", "X16"));
        uniqueMiners.add(new MinerInfo(59L, "Jasminer X16-Pro", "Jasminer", "X16"));
        uniqueMiners.add(new MinerInfo(60L, "ElphapeX DGHOME1", "ElphapeX", "DG"));
        uniqueMiners.add(new MinerInfo(62L, "Goldshell MiniDoge", "Goldshell", "MiniDoge"));
        uniqueMiners.add(new MinerInfo(63L, "Goldshell DG", "Goldshell", "DG"));
        uniqueMiners.add(new MinerInfo(68L, "Whatsminer KS3M", "MicroBT", "KS3"));
        uniqueMiners.add(new MinerInfo(72L, "Antminer P221B", "Bitmain", "P221"));
        uniqueMiners.add(new MinerInfo(73L, "Antminer P221C", "Bitmain", "P221"));
        uniqueMiners.add(new MinerInfo(67L, "Whatsminer M33S++", "MicroBT", "M33S"));
        uniqueMiners.add(new MinerInfo(79L, "Antminer S19i", "Bitmain", "S19"));
        uniqueMiners.add(new MinerInfo(82L, "Whatsminer M60S+", "MicroBT", "M60"));
        uniqueMiners.add(new MinerInfo(83L, "Whatsminer X4-Q", "MicroBT", "X4"));
        uniqueMiners.add(new MinerInfo(89L, "Whatsminer M61S", "MicroBT", "M61"));
        uniqueMiners.add(new MinerInfo(86L, "Goldshell D9", "Goldshell", "D9")); // предположение
        
        int created = 0;
        for (MinerInfo miner : uniqueMiners) {
            try {
                Optional<Product> productOpt = productRepository.findById(miner.productId);
                if (productOpt.isEmpty()) {
                    log.warn("⚠️ Product ID={} не найден, пропускаем", miner.productId);
                    continue;
                }
                
                Product product = productOpt.get();
                
                // Проверяем, не связан ли уже с MinerDetail
                if (product.getMinerDetail() != null) {
                    log.debug("⏭️ Product ID={} ({}) уже связан с MinerDetail ID={}, пропускаем", 
                            miner.productId, product.getModel(), product.getMinerDetail().getId());
                    continue;
                }
                
                // Создаем MinerDetail для уникального майнера
                MinerDetail minerDetail = new MinerDetail();
                minerDetail.setStandardName(miner.standardName);
                minerDetail.setManufacturer(miner.manufacturer);
                minerDetail.setSeries(miner.series);
                
                MinerDetail saved = minerDetailRepository.save(minerDetail);
                log.info("✅ Создан MinerDetail ID={} для уникального майнера: {} (Product ID={})", 
                        saved.getId(), miner.standardName, miner.productId);
                
                // Связываем товар с MinerDetail
                product.setMinerDetail(saved);
                productRepository.save(product);
                
                created++;
            } catch (Exception e) {
                log.error("❌ Ошибка при создании MinerDetail для Product ID={}: {}", 
                        miner.productId, e.getMessage(), e);
            }
        }
        
        return created;
    }
    
    /**
     * Вспомогательный класс для информации о группе
     */
    private static class GroupInfo {
        String standardName;
        String manufacturer;
        String series;
        java.util.List<Long> productIds;
        
        GroupInfo(String standardName, String manufacturer, String series, java.util.List<Long> productIds) {
            this.standardName = standardName;
            this.manufacturer = manufacturer;
            this.series = series;
            this.productIds = productIds;
        }
    }
    
    /**
     * Вспомогательный класс для информации об уникальном майнере
     */
    private static class MinerInfo {
        Long productId;
        String standardName;
        String manufacturer;
        String series;
        
        MinerInfo(Long productId, String standardName, String manufacturer, String series) {
            this.productId = productId;
            this.standardName = standardName;
            this.manufacturer = manufacturer;
            this.series = series;
        }
    }
}

