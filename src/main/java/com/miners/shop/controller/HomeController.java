package com.miners.shop.controller;

import com.miners.shop.entity.MinerDetail;
import com.miners.shop.entity.Offer;
import com.miners.shop.entity.OperationType;
import com.miners.shop.entity.Product;
import com.miners.shop.entity.WhatsAppMessage;
import com.miners.shop.repository.MinerDetailRepository;
import com.miners.shop.repository.OfferRepository;
import com.miners.shop.repository.ProductRepository;
import com.miners.shop.service.WhatsAppMessageService;
import com.miners.shop.util.ImageUrlResolver;
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
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;
import com.miners.shop.util.SeoUtil;
import com.miners.shop.util.SchemaOrgUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class HomeController {
    
    private final WhatsAppMessageService messageService;
    private final ProductRepository productRepository;
    private final OfferRepository offerRepository;
    private final MinerDetailRepository minerDetailRepository;
    private final ImageUrlResolver imageUrlResolver;
    
    @GetMapping("/")
    @Transactional(readOnly = true)
    public String home(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(required = false) String chatType,
            HttpServletRequest request,
            Model model) {
        
        long startTime = System.currentTimeMillis();
        
        // Статистика сообщений
        long totalMessages = messageService.getTotalMessages();
        long groupMessages = messageService.getMessagesCountByType("group");
        long personalMessages = messageService.getMessagesCountByType("personal");
        
        // Получаем MinerDetail с предложениями для Bitmain и MicroBT
        List<MinerDetail> bitmainMinerDetails = minerDetailRepository.findAllWithOffersByManufacturers(List.of("Bitmain"));
        List<MinerDetail> microbtMinerDetails = minerDetailRepository.findAllWithOffersByManufacturers(List.of("MicroBT"));
        
        // ОПТИМИЗАЦИЯ: Загружаем все продукты и предложения одним батч-запросом
        List<Long> allMinerDetailIds = new java.util.ArrayList<>();
        allMinerDetailIds.addAll(bitmainMinerDetails.stream().map(MinerDetail::getId).toList());
        allMinerDetailIds.addAll(microbtMinerDetails.stream().map(MinerDetail::getId).toList());
        
        // Загружаем все продукты для всех MinerDetail одним запросом
        List<Product> allProducts = productRepository.findByMinerDetailIdIn(allMinerDetailIds);
        Map<Long, List<Product>> productsByMinerDetailId = allProducts.stream()
                .collect(Collectors.groupingBy(p -> p.getMinerDetail().getId()));
        
        // Загружаем все предложения одним запросом
        List<Long> allProductIds = allProducts.stream().map(Product::getId).toList();
        final Map<Long, List<Offer>> offersByProductId;
        if (!allProductIds.isEmpty()) {
            List<Offer> allOffers = offerRepository.findByProductIdIn(allProductIds);
            offersByProductId = allOffers.stream()
                    .collect(Collectors.groupingBy(o -> o.getProduct().getId()));
        } else {
            offersByProductId = new HashMap<>();
        }
        
        // Вычисляем количество предложений для каждого MinerDetail
        Map<Long, Integer> offersCountByMinerDetailId = new HashMap<>();
        for (Map.Entry<Long, List<Product>> entry : productsByMinerDetailId.entrySet()) {
            int totalOffers = entry.getValue().stream()
                    .mapToInt(p -> offersByProductId.getOrDefault(p.getId(), List.of()).size())
                    .sum();
            offersCountByMinerDetailId.put(entry.getKey(), totalOffers);
        }
        
        // Сортируем по количеству предложений (по убыванию) и берем первые 4
        List<MinerDetail> topBitmain = bitmainMinerDetails.stream()
                .sorted((md1, md2) -> Integer.compare(
                    offersCountByMinerDetailId.getOrDefault(md2.getId(), 0),
                    offersCountByMinerDetailId.getOrDefault(md1.getId(), 0)))
                .limit(4)
                .collect(Collectors.toList());
        
        List<MinerDetail> topMicroBT = microbtMinerDetails.stream()
                .sorted((md1, md2) -> Integer.compare(
                    offersCountByMinerDetailId.getOrDefault(md2.getId(), 0),
                    offersCountByMinerDetailId.getOrDefault(md1.getId(), 0)))
                .limit(4)
                .collect(Collectors.toList());
        
        // Создаем структуру данных для отображения: Map<Manufacturer, List<MinerDetail>>
        Map<String, List<MinerDetail>> minersByManufacturer = new HashMap<>();
        minersByManufacturer.put("Bitmain", topBitmain);
        minersByManufacturer.put("MicroBT", topMicroBT);
        
        // Для каждого MinerDetail вычисляем статистику (минимальная цена, количество предложений)
        // ОПТИМИЗАЦИЯ: Используем уже загруженные данные
        Map<Long, Map<String, Object>> minerStats = new HashMap<>();
        Map<Long, String> imageUrls = new HashMap<>();
        
        for (MinerDetail minerDetail : topBitmain) {
            processMinerDetailOptimized(minerDetail, productsByMinerDetailId, offersByProductId, minerStats, imageUrls);
        }
        for (MinerDetail minerDetail : topMicroBT) {
            processMinerDetailOptimized(minerDetail, productsByMinerDetailId, offersByProductId, minerStats, imageUrls);
        }
        
        long endTime = System.currentTimeMillis();
        log.debug("Время выполнения home(): {} мс", (endTime - startTime));
        
        model.addAttribute("totalMessages", totalMessages);
        model.addAttribute("groupMessages", groupMessages);
        model.addAttribute("personalMessages", personalMessages);
        model.addAttribute("minersByManufacturer", minersByManufacturer);
        model.addAttribute("minerStats", minerStats);
        model.addAttribute("imageUrls", imageUrls);
        
        // SEO meta теги
        model.addAttribute("pageTitle", "Купить майнер для майнинга - ASIC майнеры Bitmain, MicroBT | MinerHive");
        model.addAttribute("pageDescription", "Купить майнер для майнинга криптовалют. Широкий выбор ASIC майнеров от Bitmain, MicroBT, Canaan. Новые и б/у майнеры с гарантией. Где купить майнер в Москве - доставка по всей России.");
        model.addAttribute("pageKeywords", "купить майнер, майнер купить, где купить майнер, asic майнер, майнер для майнинга, майнер биткоин, bitmain, antminer");
        model.addAttribute("canonicalUrl", SeoUtil.generateCanonicalUrl(request));
        model.addAttribute("ogImage", SeoUtil.getBaseUrl() + "/assets/images/logo/logo.png");
        
        // Schema.org разметка
        model.addAttribute("organizationSchema", SchemaOrgUtil.generateOrganizationSchema());
        model.addAttribute("websiteSchema", SchemaOrgUtil.generateWebSiteSchema());
        
        return "index-new";
    }
    
    /**
     * Обрабатывает MinerDetail и вычисляет статистику (минимальная цена, количество предложений)
     * Ищет минимальную цену сначала за последние 24 часа, если нет - за все время
     * Не показывает цену 0
     * ОПТИМИЗИРОВАННАЯ ВЕРСИЯ: использует уже загруженные данные вместо новых запросов к БД
     */
    private void processMinerDetailOptimized(
            MinerDetail minerDetail,
            Map<Long, List<Product>> productsByMinerDetailId,
            Map<Long, List<Offer>> offersByProductId,
            Map<Long, Map<String, Object>> minerStats,
            Map<Long, String> imageUrls) {
        
        List<Product> linkedProducts = productsByMinerDetailId.getOrDefault(minerDetail.getId(), List.of());
        List<Offer> allOffers = linkedProducts.stream()
                .flatMap(p -> offersByProductId.getOrDefault(p.getId(), List.of()).stream())
                .collect(Collectors.toList());
        
        BigDecimal minPrice = null;
        int totalOffersCount = allOffers.size();
        String currency = "RUB";
        
        if (!allOffers.isEmpty()) {
            // Фильтруем только предложения на продажу (SELL)
            List<Offer> sellOffers = allOffers.stream()
                    .filter(offer -> offer.getOperationType() == OperationType.SELL)
                    .collect(Collectors.toList());
            
            if (!sellOffers.isEmpty()) {
                // Получаем валюту из первого предложения
                if (sellOffers.get(0).getCurrency() != null) {
                    currency = sellOffers.get(0).getCurrency();
                }
                
                // Сначала ищем предложения за последние 24 часа
                LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
                List<Offer> recentOffers = sellOffers.stream()
                        .filter(offer -> offer.getUpdatedAt() != null && offer.getUpdatedAt().isAfter(oneDayAgo))
                        .filter(offer -> offer.getPrice() != null && offer.getPrice().compareTo(BigDecimal.ZERO) > 0)
                        .collect(Collectors.toList());
                
                if (!recentOffers.isEmpty()) {
                    // Находим минимальную цену за последние 24 часа
                    minPrice = recentOffers.stream()
                            .map(Offer::getPrice)
                            .min(BigDecimal::compareTo)
                            .orElse(null);
                } else {
                    // Если за сутки нет предложений, ищем во всех предложениях
                    minPrice = sellOffers.stream()
                            .filter(offer -> offer.getPrice() != null && offer.getPrice().compareTo(BigDecimal.ZERO) > 0)
                            .map(Offer::getPrice)
                            .min(BigDecimal::compareTo)
                            .orElse(null);
                }
            }
        }
        
        Map<String, Object> stats = new HashMap<>();
        // Сохраняем цену только если она не null и не 0
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) > 0) {
            stats.put("minPrice", minPrice);
        } else {
            stats.put("minPrice", null);
        }
        stats.put("offersCount", totalOffersCount);
        stats.put("currency", currency);
        minerStats.put(minerDetail.getId(), stats);
        
        // Устанавливаем URL изображения: сначала проверяем imageUrl из MinerDetail, если нет - используем ImageUrlResolver
        String imageUrl = null;
        if (minerDetail.getImageUrl() != null && !minerDetail.getImageUrl().trim().isEmpty()) {
            imageUrl = minerDetail.getImageUrl();
        } else {
            imageUrl = imageUrlResolver.resolveImageUrl(minerDetail.getStandardName());
        }
        imageUrls.put(minerDetail.getId(), imageUrl);
    }
    
    /**
     * API endpoint для получения сообщений в JSON формате (для AJAX обновления)
     */
    @GetMapping(value = "/api/messages", produces = "application/json;charset=UTF-8")
    public ResponseEntity<?> getMessagesJson(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(required = false) String chatType) {
        
        // Создаем Pageable с сортировкой по времени (новые сначала)
        int defaultSize = Math.max(size, 100);
        Pageable pageable = PageRequest.of(page, defaultSize, Sort.by(Sort.Direction.DESC, "timestamp"));
        
        Page<WhatsAppMessage> messages;
        
        if (chatType != null && !chatType.isEmpty()) {
            messages = messageService.getMessagesByChatType(chatType, pageable);
        } else {
            messages = messageService.getAllMessages(pageable);
        }
        
        // Статистика
        long totalMessages = messageService.getTotalMessages();
        long groupMessages = messageService.getMessagesCountByType("group");
        long personalMessages = messageService.getMessagesCountByType("personal");
        
        // Формируем ответ
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("messages", messages.getContent());
        response.put("currentPage", page);
        response.put("totalPages", messages.getTotalPages());
        response.put("totalElements", messages.getTotalElements());
        response.put("totalMessages", totalMessages);
        response.put("groupMessages", groupMessages);
        response.put("personalMessages", personalMessages);
        response.put("chatType", chatType != null ? chatType : "");
        
        return ResponseEntity.ok().body(response);
    }
}
