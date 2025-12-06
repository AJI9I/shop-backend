package com.miners.shop.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miners.shop.entity.MinerDetail;
import com.miners.shop.entity.Offer;
import com.miners.shop.entity.OperationType;

import java.math.BigDecimal;
import java.util.*;

/**
 * Утилита для генерации Schema.org разметки (JSON-LD)
 */
public class SchemaOrgUtil {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String BASE_URL = "https://minerhive.ru";
    
    /**
     * Генерирует Schema.org разметку для Organization (главная страница)
     */
    public static String generateOrganizationSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("@context", "https://schema.org");
        schema.put("@type", "Organization");
        schema.put("name", "MinerHive");
        schema.put("url", BASE_URL);
        schema.put("logo", BASE_URL + "/assets/images/logo/logo.png");
        schema.put("description", "Интернет-магазин майнинг-оборудования. Широкий выбор ASIC майнеров от Bitmain, MicroBT, Canaan.");
        
        Map<String, Object> contactPoint = new LinkedHashMap<>();
        contactPoint.put("@type", "ContactPoint");
        contactPoint.put("contactType", "customer service");
        schema.put("contactPoint", contactPoint);
        
        return toJsonLd(schema);
    }
    
    /**
     * Генерирует Schema.org разметку для WebSite (главная страница)
     */
    public static String generateWebSiteSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("@context", "https://schema.org");
        schema.put("@type", "WebSite");
        schema.put("name", "MinerHive");
        schema.put("url", BASE_URL);
        
        Map<String, Object> potentialAction = new LinkedHashMap<>();
        potentialAction.put("@type", "SearchAction");
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("@type", "EntryPoint");
        target.put("urlTemplate", BASE_URL + "/products?search={search_term_string}");
        potentialAction.put("target", target);
        schema.put("potentialAction", potentialAction);
        
        return toJsonLd(schema);
    }
    
    /**
     * Генерирует Schema.org разметку для Product (страница товара)
     */
    public static String generateProductSchema(MinerDetail minerDetail, List<Offer> offers, String imageUrl) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("@context", "https://schema.org");
        schema.put("@type", "Product");
        
        // Название товара
        schema.put("name", minerDetail.getStandardName() != null ? minerDetail.getStandardName() : "Майнер");
        
        // Описание
        if (minerDetail.getDescription() != null && !minerDetail.getDescription().isEmpty()) {
            schema.put("description", minerDetail.getDescription());
        } else {
            schema.put("description", "ASIC майнер для майнинга криптовалют");
        }
        
        // Изображение
        if (imageUrl != null && !imageUrl.isEmpty()) {
            schema.put("image", imageUrl);
        } else {
            schema.put("image", BASE_URL + "/assets/images/logo/logo.png");
        }
        
        // Бренд
        if (minerDetail.getManufacturer() != null && !minerDetail.getManufacturer().isEmpty()) {
            Map<String, Object> brand = new LinkedHashMap<>();
            brand.put("@type", "Brand");
            brand.put("name", minerDetail.getManufacturer());
            schema.put("brand", brand);
        }
        
        // Характеристики
        List<Map<String, Object>> additionalProperty = new ArrayList<>();
        if (minerDetail.getHashrate() != null && !minerDetail.getHashrate().isEmpty()) {
            Map<String, Object> prop = new LinkedHashMap<>();
            prop.put("@type", "PropertyValue");
            prop.put("name", "Хэшрейт");
            prop.put("value", minerDetail.getHashrate());
            additionalProperty.add(prop);
        }
        if (minerDetail.getPowerConsumption() != null && !minerDetail.getPowerConsumption().isEmpty()) {
            Map<String, Object> prop = new LinkedHashMap<>();
            prop.put("@type", "PropertyValue");
            prop.put("name", "Потребление энергии");
            prop.put("value", minerDetail.getPowerConsumption());
            additionalProperty.add(prop);
        }
        if (minerDetail.getAlgorithm() != null && !minerDetail.getAlgorithm().isEmpty()) {
            Map<String, Object> prop = new LinkedHashMap<>();
            prop.put("@type", "PropertyValue");
            prop.put("name", "Алгоритм");
            prop.put("value", minerDetail.getAlgorithm());
            additionalProperty.add(prop);
        }
        if (!additionalProperty.isEmpty()) {
            schema.put("additionalProperty", additionalProperty);
        }
        
        // Предложения (AggregateOffer)
        List<Offer> sellOffers = offers.stream()
                .filter(o -> o.getOperationType() == OperationType.SELL)
                .filter(o -> o.getPrice() != null && o.getPrice().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        
        if (!sellOffers.isEmpty()) {
            Map<String, Object> aggregateOffer = new LinkedHashMap<>();
            aggregateOffer.put("@type", "AggregateOffer");
            
            // Минимальная и максимальная цена
            Optional<BigDecimal> minPrice = sellOffers.stream()
                    .map(Offer::getPrice)
                    .min(BigDecimal::compareTo);
            Optional<BigDecimal> maxPrice = sellOffers.stream()
                    .map(Offer::getPrice)
                    .max(BigDecimal::compareTo);
            
            if (minPrice.isPresent()) {
                aggregateOffer.put("lowPrice", minPrice.get().toString());
            }
            if (maxPrice.isPresent()) {
                aggregateOffer.put("highPrice", maxPrice.get().toString());
            }
            
            aggregateOffer.put("priceCurrency", "RUB");
            aggregateOffer.put("availability", "https://schema.org/InStock");
            aggregateOffer.put("offerCount", sellOffers.size());
            
            schema.put("offers", aggregateOffer);
        }
        
        // URL товара
        String slug = minerDetail.getSlug() != null && !minerDetail.getSlug().isEmpty() 
                ? minerDetail.getSlug() 
                : String.valueOf(minerDetail.getId());
        schema.put("url", BASE_URL + "/products/" + slug);
        
        return toJsonLd(schema);
    }
    
    /**
     * Генерирует Schema.org разметку для BreadcrumbList
     */
    public static String generateBreadcrumbSchema(List<Map<String, String>> breadcrumbs) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("@context", "https://schema.org");
        schema.put("@type", "BreadcrumbList");
        
        List<Map<String, Object>> itemListElement = new ArrayList<>();
        int position = 1;
        for (Map<String, String> breadcrumb : breadcrumbs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("@type", "ListItem");
            item.put("position", position);
            item.put("name", breadcrumb.get("name"));
            item.put("item", BASE_URL + breadcrumb.get("url"));
            itemListElement.add(item);
            position++;
        }
        
        schema.put("itemListElement", itemListElement);
        
        return toJsonLd(schema);
    }
    
    /**
     * Преобразует Map в JSON-LD строку
     */
    private static String toJsonLd(Map<String, Object> schema) {
        try {
            return objectMapper.writeValueAsString(schema);
        } catch (Exception e) {
            return "";
        }
    }
}

