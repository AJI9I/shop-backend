package com.miners.shop.controller;

import com.miners.shop.entity.Offer;
import com.miners.shop.entity.Product;
import com.miners.shop.entity.WhatsAppMessage;
import com.miners.shop.repository.OfferRepository;
import com.miners.shop.repository.ProductRepository;
import com.miners.shop.service.WhatsAppMessageService;
import com.miners.shop.util.ImageUrlResolver;
import lombok.RequiredArgsConstructor;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class HomeController {
    
    private final WhatsAppMessageService messageService;
    private final ProductRepository productRepository;
    private final OfferRepository offerRepository;
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
        
        // Получаем товары для главной страницы (первые 12) без eagerly загруженных offers
        Pageable productPageable = PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Product> productsPage = productRepository.findAll(productPageable);
        List<Product> products = productsPage.getContent();
        
        // Получаем ID всех продуктов
        List<Long> productIds = products.stream()
                .map(Product::getId)
                .toList();
        
        // Загружаем все offers для этих продуктов одним запросом
        final Map<Long, List<Offer>> offersByProductId;
        if (!productIds.isEmpty()) {
            List<Offer> allOffers = offerRepository.findByProductIdIn(productIds);
            offersByProductId = allOffers.stream()
                    .collect(java.util.stream.Collectors.groupingBy(o -> o.getProduct().getId()));
        } else {
            offersByProductId = new HashMap<>();
        }
        
        // Вычисляем минимальную цену для каждого товара
        for (Product product : products) {
            // Получаем offers для этого продукта
            List<Offer> productOffers = offersByProductId.getOrDefault(product.getId(), List.of());
            product.getOffers().clear();
            product.getOffers().addAll(productOffers);
            
            // Вычисляем минимальную цену для продажи
            BigDecimal minPrice = productOffers.stream()
                    .filter(offer -> offer.getPrice() != null 
                            && offer.getOperationType() != null 
                            && offer.getOperationType().name().equals("SELL"))
                    .map(Offer::getPrice)
                    .min(BigDecimal::compareTo)
                    .orElse(null);
            product.setMinPrice(minPrice);
            
            // Устанавливаем URL изображения на основе модели товара
            // Если есть изображение в папке img - используем его, иначе placeholder
            String imageUrl = imageUrlResolver.resolveImageUrl(product.getModel());
            product.setImageUrl(imageUrl); // null означает использование placeholder в шаблоне
        }
        
        model.addAttribute("totalMessages", totalMessages);
        model.addAttribute("groupMessages", groupMessages);
        model.addAttribute("personalMessages", personalMessages);
        model.addAttribute("products", products);
        
        return "index-new";
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
