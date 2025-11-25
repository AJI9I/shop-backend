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
            Model model) {
        
        // Статистика сообщений
        long totalMessages = messageService.getTotalMessages();
        long groupMessages = messageService.getMessagesCountByType("group");
        long personalMessages = messageService.getMessagesCountByType("personal");
        
        // Получаем MinerDetail с предложениями для Bitmain (первые 4 с наибольшим количеством предложений)
        List<MinerDetail> bitmainMinerDetails = minerDetailRepository.findAllWithOffersByManufacturers(List.of("Bitmain"));
        List<MinerDetail> microbtMinerDetails = minerDetailRepository.findAllWithOffersByManufacturers(List.of("MicroBT"));
        
        // Функция для подсчета количества предложений и сортировки
        java.util.function.Function<MinerDetail, Integer> countOffers = minerDetail -> {
            List<Product> linkedProducts = productRepository.findByMinerDetailId(minerDetail.getId());
            List<Long> productIds = linkedProducts.stream().map(Product::getId).toList();
            if (productIds.isEmpty()) return 0;
            List<Offer> offers = offerRepository.findByProductIdIn(productIds);
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
        
        for (MinerDetail minerDetail : topBitmain) {
            processMinerDetail(minerDetail, minerStats, imageUrls);
        }
        for (MinerDetail minerDetail : topMicroBT) {
            processMinerDetail(minerDetail, minerStats, imageUrls);
        }
        
        model.addAttribute("totalMessages", totalMessages);
        model.addAttribute("groupMessages", groupMessages);
        model.addAttribute("personalMessages", personalMessages);
        model.addAttribute("minersByManufacturer", minersByManufacturer);
        model.addAttribute("minerStats", minerStats);
        model.addAttribute("imageUrls", imageUrls);
        
        return "index-new";
    }
    
    /**
     * Обрабатывает MinerDetail и вычисляет статистику (минимальная цена, количество предложений)
     * Ищет минимальную цену сначала за последние 24 часа, если нет - за все время
     * Не показывает цену 0
     */
    private void processMinerDetail(MinerDetail minerDetail, Map<Long, Map<String, Object>> minerStats, Map<Long, String> imageUrls) {
        List<Product> linkedProducts = productRepository.findByMinerDetailId(minerDetail.getId());
        List<Long> productIds = linkedProducts.stream().map(Product::getId).toList();
        
        BigDecimal minPrice = null;
        int totalOffersCount = 0;
        String currency = "RUB";
        
        if (!productIds.isEmpty()) {
            List<Offer> allOffers = offerRepository.findByProductIdIn(productIds);
            totalOffersCount = allOffers.size();
            
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
