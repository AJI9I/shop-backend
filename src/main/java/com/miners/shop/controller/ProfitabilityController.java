package com.miners.shop.controller;

import com.miners.shop.entity.MinerDetail;
import com.miners.shop.entity.OperationType;
import com.miners.shop.entity.Product;
import com.miners.shop.repository.OfferRepository;
import com.miners.shop.repository.ProductRepository;
import com.miners.shop.util.ImageUrlResolver;
import com.miners.shop.util.MinerDataParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Контроллер для страницы расчета доходности ASIC майнеров
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ProfitabilityController {
    
    private final ProductRepository productRepository;
    private final OfferRepository offerRepository;
    private final ImageUrlResolver imageUrlResolver;
    
    /**
     * Страница "Расчет доходности ASIC"
     */
    @GetMapping("/private/profitability")
    @Transactional(readOnly = true)
    public String profitability(Model model) {
        log.info("Открыта страница расчета доходности ASIC");
        
        try {
            // Получаем только товары с MinerDetail для калькулятора
            // Используем запрос, который загружает MinerDetail сразу
            List<Product> allProducts = productRepository.findAll();
            List<Map<String, Object>> minersForCalculator = new ArrayList<>();
            
            // Получаем ID всех продуктов с MinerDetail
            List<Long> productIds = new ArrayList<>();
            for (Product product : allProducts) {
                if (product.getMinerDetail() != null) {
                    productIds.add(product.getId());
                }
            }
            
            // Загружаем минимальные цены для всех продуктов одним запросом
            Map<Long, BigDecimal> minPricesByProductId = new HashMap<>();
            if (!productIds.isEmpty()) {
                try {
                    // Загружаем все offers для этих продуктов
                    List<com.miners.shop.entity.Offer> allOffers = offerRepository.findByProductIdIn(productIds);
                    // Группируем по productId и находим минимальную цену для SELL
                    for (com.miners.shop.entity.Offer offer : allOffers) {
                        if (offer.getProduct() != null && 
                            offer.getOperationType() == OperationType.SELL && 
                            offer.getPrice() != null) {
                            Long productId = offer.getProduct().getId();
                            BigDecimal currentMin = minPricesByProductId.get(productId);
                            if (currentMin == null || offer.getPrice().compareTo(currentMin) < 0) {
                                minPricesByProductId.put(productId, offer.getPrice());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Не удалось загрузить минимальные цены: {}", e.getMessage());
                    // Продолжаем без цен
                }
            }
            
            // Формируем данные для калькулятора
            for (Product product : allProducts) {
                MinerDetail detail = product.getMinerDetail();
                if (detail != null) {
                    try {
                        Map<String, Object> minerData = new HashMap<>();
                        minerData.put("id", product.getId());
                        minerData.put("model", product.getModel() != null ? product.getModel() : "");
                        minerData.put("standardName", detail.getStandardName() != null ? detail.getStandardName() : product.getModel());
                        minerData.put("manufacturer", detail.getManufacturer() != null ? detail.getManufacturer() : "");
                        
                        // Парсим хешрейт и энергопотребление
                        Double hashrate = MinerDataParser.parseHashrate(detail.getHashrate());
                        Integer power = MinerDataParser.parsePowerConsumption(detail.getPowerConsumption());
                        
                        minerData.put("hashrate", hashrate != null ? hashrate : 0);
                        minerData.put("hashrateStr", detail.getHashrate() != null ? detail.getHashrate() : "");
                        minerData.put("power", power != null ? power : 0);
                        minerData.put("powerStr", detail.getPowerConsumption() != null ? detail.getPowerConsumption() : "");
                        minerData.put("algorithm", detail.getAlgorithm() != null ? detail.getAlgorithm() : "");
                        minerData.put("coins", detail.getCoins() != null ? detail.getCoins() : "");
                        
                        // Получаем изображение
                        String imageUrl = imageUrlResolver.resolveImageUrl(product.getModel());
                        minerData.put("imageUrl", imageUrl != null ? imageUrl : "");
                        
                        // Получаем минимальную цену из предзагруженных данных
                        BigDecimal minPrice = minPricesByProductId.get(product.getId());
                        if (minPrice != null) {
                            minerData.put("minPrice", minPrice.doubleValue());
                        } else {
                            minerData.put("minPrice", 0.0);
                        }
                        
                        minersForCalculator.add(minerData);
                    } catch (Exception e) {
                        log.error("Ошибка при обработке продукта ID={}: {}", product.getId(), e.getMessage(), e);
                        // Пропускаем этот продукт, но продолжаем обработку остальных
                    }
                }
            }
            
            model.addAttribute("pageTitle", "Расчет доходности ASIC - MinerHive");
            model.addAttribute("miners", minersForCalculator);
            
            log.info("Загружено {} майнеров для калькулятора", minersForCalculator.size());
            
        } catch (Exception e) {
            log.error("Критическая ошибка при загрузке данных для калькулятора: {}", e.getMessage(), e);
            // Возвращаем пустой список, чтобы страница все равно загрузилась
            model.addAttribute("pageTitle", "Расчет доходности ASIC - MinerHive");
            model.addAttribute("miners", new ArrayList<>());
        }
        
        return "profitability";
    }
}

