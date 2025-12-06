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
import com.miners.shop.util.SchemaOrgUtil;
import com.miners.shop.util.SeoUtil;
import jakarta.servlet.http.HttpServletRequest;
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
        
        // Статистика сообщений
        long totalMessages = messageService.getTotalMessages();
        long groupMessages = messageService.getMessagesCountByType("group");
        long personalMessages = messageService.getMessagesCountByType("personal");
        
        // Получаем MinerDetail с предложениями для Bitmain (первые 4 с наибольшим количеством предложений)
        List<MinerDetail> bitmainMinerDetails = minerDetailRepository.findAllWithOffersByManufacturers(List.of("Bitmain"));
        List<MinerDetail> microbtMinerDetails = minerDetailRepository.findAllWithOffersByManufacturers(List.of("MicroBT"));
        
        // ОПТИМИЗАЦИЯ: Загружаем все данные одним запросом вместо N+1
        List<MinerDetail> allMinerDetails = new java.util.ArrayList<>();
        allMinerDetails.addAll(bitmainMinerDetails);
        allMinerDetails.addAll(microbtMinerDetails);
        
        // Собираем все ID MinerDetail
        List<Long> minerDetailIds = allMinerDetails.stream()
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
        
        // Группируем offers по productId
        Map<Long, List<Offer>> offersByProductId = allOffers.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getProduct() != null ? o.getProduct().getId() : null,
                        Collectors.toList()
                ));
        
        // Группируем Product по minerDetailId
        Map<Long, List<Product>> productsByMinerDetailId = allProducts.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getMinerDetail() != null ? p.getMinerDetail().getId() : null,
                        Collectors.toList()
                ));
        
        // Группируем offers по minerDetailId
        Map<Long, List<Offer>> offersByMinerDetailId = new HashMap<>();
        for (Product product : allProducts) {
            if (product.getMinerDetail() != null) {
                Long minerDetailId = product.getMinerDetail().getId();
                List<Offer> productOffers = offersByProductId.getOrDefault(product.getId(), List.of());
                offersByMinerDetailId.computeIfAbsent(minerDetailId, k -> new java.util.ArrayList<>())
                        .addAll(productOffers);
            }
        }
        
        // Функция для подсчета количества предложений (использует предзагруженные данные)
        java.util.function.Function<MinerDetail, Integer> countOffers = minerDetail -> {
            List<Offer> offers = offersByMinerDetailId.getOrDefault(minerDetail.getId(), List.of());
            return offers.size();
        };
        
        // Сортируем по количеству предложений (по убыванию) и берем первые 4
        List<MinerDetail> topBitmain = bitmainMinerDetails.stream()
                .sorted((md1, md2) -> Integer.compare(countOffers.apply(md2), countOffers.apply(md1)))
                .limit(4)
                .collect(Collectors.toList());
        
        List<MinerDetail> topMicroBT = microbtMinerDetails.stream()
                .sorted((md1, md2) -> Integer.compare(countOffers.apply(md2), countOffers.apply(md1)))
                .limit(4)
                .collect(Collectors.toList());
        
        // Создаем структуру данных для отображения: Map<Manufacturer, List<MinerDetail>>
        Map<String, List<MinerDetail>> minersByManufacturer = new HashMap<>();
        minersByManufacturer.put("Bitmain", topBitmain);
        minersByManufacturer.put("MicroBT", topMicroBT);
        
        // Для каждого MinerDetail вычисляем статистику (минимальная цена, количество предложений)
        Map<Long, Map<String, Object>> minerStats = new HashMap<>();
        Map<Long, String> imageUrls = new HashMap<>();
        
        // Используем предзагруженные данные для обработки
        for (MinerDetail minerDetail : topBitmain) {
            processMinerDetailOptimized(minerDetail, minerStats, imageUrls, 
                    productsByMinerDetailId.getOrDefault(minerDetail.getId(), List.of()),
                    offersByMinerDetailId.getOrDefault(minerDetail.getId(), List.of()));
        }
        for (MinerDetail minerDetail : topMicroBT) {
            processMinerDetailOptimized(minerDetail, minerStats, imageUrls,
                    productsByMinerDetailId.getOrDefault(minerDetail.getId(), List.of()),
                    offersByMinerDetailId.getOrDefault(minerDetail.getId(), List.of()));
        }
        
        model.addAttribute("totalMessages", totalMessages);
        model.addAttribute("groupMessages", groupMessages);
        model.addAttribute("personalMessages", personalMessages);
        model.addAttribute("minersByManufacturer", minersByManufacturer);
        model.addAttribute("minerStats", minerStats);
        model.addAttribute("imageUrls", imageUrls);
        
        // Schema.org разметка для главной страницы
        model.addAttribute("organizationSchema", SchemaOrgUtil.generateOrganizationSchema());
        model.addAttribute("websiteSchema", SchemaOrgUtil.generateWebSiteSchema());
        
        // SEO мета-теги
        model.addAttribute("pageTitle", "MinerHive - Интернет-магазин майнинг-оборудования");
        model.addAttribute("pageDescription", "Купить майнер для майнинга криптовалют. Широкий выбор ASIC майнеров от Bitmain, MicroBT, Canaan. Новые и б/у майнеры с гарантией.");
        model.addAttribute("pageKeywords", "майнер, ASIC майнер, купить майнер, Bitmain, MicroBT, майнинг, криптовалюта");
        model.addAttribute("canonicalUrl", SeoUtil.generateCanonicalUrl(request));
        model.addAttribute("ogImage", "https://minerhive.ru/assets/images/logo/logo.png");
        
        return "index-new";
    }
    
    /**
     * ОПТИМИЗИРОВАННАЯ версия обработки MinerDetail с предзагруженными данными
     * Обрабатывает MinerDetail и вычисляет статистику (минимальная цена, количество предложений)
     * Ищет минимальную цену сначала за последние 24 часа, если нет - за все время
     * Не показывает цену 0
     */
    private void processMinerDetailOptimized(MinerDetail minerDetail, 
                                             Map<Long, Map<String, Object>> minerStats, 
                                             Map<Long, String> imageUrls,
                                             List<Product> linkedProducts,
                                             List<Offer> allOffers) {
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
