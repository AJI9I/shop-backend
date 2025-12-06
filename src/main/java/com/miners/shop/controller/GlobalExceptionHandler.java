package com.miners.shop.controller;

import com.miners.shop.entity.MinerDetail;
import com.miners.shop.entity.NotFoundError;
import com.miners.shop.entity.Offer;
import com.miners.shop.entity.OperationType;
import com.miners.shop.entity.Product;
import com.miners.shop.entity.Redirect;
import com.miners.shop.repository.MinerDetailRepository;
import com.miners.shop.repository.NotFoundErrorRepository;
import com.miners.shop.repository.OfferRepository;
import com.miners.shop.repository.ProductRepository;
import com.miners.shop.repository.RedirectRepository;
import com.miners.shop.util.ImageUrlResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import jakarta.servlet.http.HttpServletResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Глобальный обработчик исключений для обработки 404 ошибок
 */
@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final RedirectRepository redirectRepository;
    private final NotFoundErrorRepository notFoundErrorRepository;
    private final OfferRepository offerRepository;
    private final MinerDetailRepository minerDetailRepository;
    private final ProductRepository productRepository;
    private final ImageUrlResolver imageUrlResolver;

    /**
     * Обработка NoHandlerFoundException (404 ошибка)
     * Выбрасывается когда Spring Boot не находит обработчик для запроса
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @Transactional
    public String handleNoHandlerFoundException(NoHandlerFoundException ex, HttpServletRequest request, HttpServletResponse response, Model model) {
        String url = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullUrl = queryString != null && !queryString.isEmpty() ? url + "?" + queryString : url;
        
        log.warn("404 Not Found (NoHandlerFoundException): {}", fullUrl);
        
        // Устанавливаем HTTP статус 404
        response.setStatus(HttpStatus.NOT_FOUND.value());
        
        // Логируем 404 ошибку
        logNotFoundError(request, fullUrl);
        
        // Проверяем, есть ли редирект для этого URL
        Optional<Redirect> redirectOpt = redirectRepository.findByFromUrlAndActiveTrue(fullUrl);
        if (redirectOpt.isPresent()) {
            Redirect redirect = redirectOpt.get();
            log.info("Найден редирект для URL {} -> {}", fullUrl, redirect.getToUrl());
            return "redirect:" + redirect.getToUrl();
        }
        
        // Если редиректа нет, показываем кастомную страницу 404
        model.addAttribute("url", fullUrl);
        model.addAttribute("pageTitle", "Страница не найдена - MinerHive");
        model.addAttribute("pageDescription", "Запрашиваемая страница не найдена");
        return "error/404";
    }

    /**
     * Обработка NoResourceFoundException (404 ошибка для статических ресурсов)
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @Transactional
    public String handleNoResourceFoundException(NoResourceFoundException ex, HttpServletRequest request, HttpServletResponse response, Model model) {
        String url = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullUrl = queryString != null && !queryString.isEmpty() ? url + "?" + queryString : url;
        
        log.warn("404 Not Found (NoResourceFoundException): {}", fullUrl);
        
        // Устанавливаем HTTP статус 404
        response.setStatus(HttpStatus.NOT_FOUND.value());
        
        // Логируем 404 ошибку
        logNotFoundError(request, fullUrl);
        
        // Проверяем, есть ли редирект для этого URL
        Optional<Redirect> redirectOpt = redirectRepository.findByFromUrlAndActiveTrue(fullUrl);
        if (redirectOpt.isPresent()) {
            Redirect redirect = redirectOpt.get();
            log.info("Найден редирект для URL {} -> {}", fullUrl, redirect.getToUrl());
            return "redirect:" + redirect.getToUrl();
        }
        
        // Если редиректа нет, показываем кастомную страницу 404
        model.addAttribute("url", fullUrl);
        model.addAttribute("pageTitle", "Страница не найдена - MinerHive");
        model.addAttribute("pageDescription", "Запрашиваемая страница не найдена");
        
        // Получаем последние товары для отображения
        addLatestProductsToModel(model);
        
        return "error/404";
    }

    /**
     * Логирование 404 ошибки в базу данных
     */
    private void logNotFoundError(HttpServletRequest request, String url) {
        try {
            // Проверяем, есть ли уже запись для этого URL
            Optional<NotFoundError> existingError = notFoundErrorRepository.findByUrl(url);
            
            if (existingError.isPresent()) {
                // Увеличиваем счетчик
                NotFoundError error = existingError.get();
                error.setCount(error.getCount() + 1);
                error.setLastOccurred(LocalDateTime.now());
                notFoundErrorRepository.save(error);
                log.debug("Обновлена запись 404 ошибки для URL: {}, счетчик: {}", url, error.getCount());
            } else {
                // Создаем новую запись
                NotFoundError error = new NotFoundError();
                error.setUrl(url);
                error.setHttpMethod(request.getMethod());
                error.setUserAgent(request.getHeader("User-Agent"));
                error.setIpAddress(getClientIpAddress(request));
                error.setReferer(request.getHeader("Referer"));
                error.setCount(1);
                notFoundErrorRepository.save(error);
                log.info("Создана новая запись 404 ошибки для URL: {}", url);
            }
        } catch (Exception e) {
            log.error("Ошибка при логировании 404 ошибки: {}", e.getMessage(), e);
        }
    }

    /**
     * Обработка ResponseStatusException (404, 500 и т.д.)
     * Выбрасывается когда контроллер явно указывает статус ошибки
     */
    @ExceptionHandler(ResponseStatusException.class)
    @Transactional
    public String handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request, HttpServletResponse response, Model model) {
        org.springframework.http.HttpStatusCode statusCode = ex.getStatusCode();
        int statusValue = statusCode.value();
        String url = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullUrl = queryString != null && !queryString.isEmpty() ? url + "?" + queryString : url;
        
        if (statusValue == HttpStatus.NOT_FOUND.value()) {
            log.warn("404 Not Found (ResponseStatusException): {}", fullUrl);
            
            // Устанавливаем HTTP статус 404
            response.setStatus(HttpStatus.NOT_FOUND.value());
            
            logNotFoundError(request, fullUrl);
            
            // Проверяем, есть ли редирект для этого URL
            Optional<Redirect> redirectOpt = redirectRepository.findByFromUrlAndActiveTrue(fullUrl);
            if (redirectOpt.isPresent()) {
                Redirect redirect = redirectOpt.get();
                log.info("Найден редирект для URL {} -> {}", fullUrl, redirect.getToUrl());
                return "redirect:" + redirect.getToUrl();
            }
            
            // Если редиректа нет, показываем кастомную страницу 404
            model.addAttribute("url", fullUrl);
            model.addAttribute("pageTitle", "Страница не найдена - MinerHive");
            model.addAttribute("pageDescription", "Запрашиваемая страница не найдена");
            
            // Получаем последние товары для отображения
            addLatestProductsToModel(model);
            
            return "error/404";
        }
        
        // Для других статусов показываем общую страницу ошибки
        log.error("{} {}: {}", statusValue, statusCode, fullUrl);
        model.addAttribute("pageTitle", "Ошибка - MinerHive");
        model.addAttribute("pageDescription", "Произошла ошибка: " + ex.getReason());
        
        // Получаем последние товары для отображения
        addLatestProductsToModel(model);
        
        return "error/error";
    }
    
    /**
     * Добавляет последние 5 товаров с предложениями в модель для отображения на страницах ошибок
     */
    private void addLatestProductsToModel(Model model) {
        try {
            // Получаем последние 5 предложений, отсортированных по дате обновления
            Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "updatedAt"));
            List<Offer> latestOffers = offerRepository.findAllByOrderByCreatedAtDesc(pageable).getContent();
            
            // Получаем уникальные MinerDetail из последних предложений
            Set<Long> minerDetailIds = new LinkedHashSet<>();
            for (Offer offer : latestOffers) {
                if (offer.getProduct() != null && offer.getProduct().getMinerDetail() != null) {
                    MinerDetail minerDetail = offer.getProduct().getMinerDetail();
                    if (minerDetail.getActive() == null || minerDetail.getActive()) {
                        minerDetailIds.add(minerDetail.getId());
                    }
                }
            }
            
            // Ограничиваем до 5 уникальных товаров
            List<MinerDetail> latestMinerDetails = new ArrayList<>();
            for (Long id : minerDetailIds) {
                if (latestMinerDetails.size() >= 5) break;
                minerDetailRepository.findById(id).ifPresent(latestMinerDetails::add);
            }
            
            // Вычисляем статистику для каждого товара
            Map<Long, Map<String, Object>> minerStats = new HashMap<>();
            Map<Long, String> imageUrls = new HashMap<>();
            
            for (MinerDetail minerDetail : latestMinerDetails) {
                processMinerDetailForErrorPage(minerDetail, minerStats, imageUrls);
            }
            
            model.addAttribute("latestProducts", latestMinerDetails);
            model.addAttribute("minerStats", minerStats);
            model.addAttribute("imageUrls", imageUrls);
        } catch (Exception e) {
            log.error("Ошибка при получении последних товаров для страницы ошибки: {}", e.getMessage(), e);
            model.addAttribute("latestProducts", Collections.emptyList());
            model.addAttribute("minerStats", Collections.emptyMap());
            model.addAttribute("imageUrls", Collections.emptyMap());
        }
    }
    
    /**
     * Обрабатывает MinerDetail и вычисляет статистику для страницы ошибки
     */
    private void processMinerDetailForErrorPage(MinerDetail minerDetail, Map<Long, Map<String, Object>> minerStats, Map<Long, String> imageUrls) {
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
     * Получение IP адреса клиента
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}

