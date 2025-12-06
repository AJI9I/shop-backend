package com.miners.shop.controller;

import com.miners.shop.entity.Redirect;
import com.miners.shop.repository.RedirectRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * Интерцептор для обработки редиректов перед обработкой запроса
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedirectInterceptor implements HandlerInterceptor {
    
    private final RedirectRepository redirectRepository;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Пропускаем статические ресурсы, API эндпоинты и страницы управления редиректами
        String url = request.getRequestURI();
        if (url == null || 
            url.startsWith("/img/") || 
            url.startsWith("/assets/") || 
            url.startsWith("/bootstrap-theme/") ||
            url.startsWith("/api/") ||
            url.startsWith("/private/redirects") || // Пропускаем страницу управления редиректами
            url.equals("/favicon.ico") ||
            url.equals("/robots.txt") ||
            url.equals("/sitemap.xml") ||
            url.equals("/") ||
            url.equals("/login") ||
            url.equals("/error") ||
            url.startsWith("/error/")) {
            return true; // Пропускаем статические ресурсы и общедоступные страницы
        }
        
        String queryString = request.getQueryString();
        String fullUrl = queryString != null ? url + "?" + queryString : url;
        
        try {
            // Проверяем, есть ли редирект для этого URL
            Optional<Redirect> redirectOpt = redirectRepository.findByFromUrlAndActiveTrue(fullUrl);
            if (redirectOpt.isPresent()) {
                Redirect redirect = redirectOpt.get();
                int statusCode = redirect.getRedirectType() == 301 
                        ? HttpStatus.MOVED_PERMANENTLY.value() 
                        : HttpStatus.FOUND.value();
                response.setStatus(statusCode);
                response.setHeader("Location", redirect.getToUrl());
                log.info("Выполнен редирект: {} -> {} (тип: {})", fullUrl, redirect.getToUrl(), redirect.getRedirectType());
                return false; // Прерываем обработку запроса
            }
        } catch (Exception e) {
            log.error("Ошибка при проверке редиректа для URL {}: {}", fullUrl, e.getMessage(), e);
            // В случае ошибки продолжаем обработку запроса
        }
        
        return true; // Продолжаем обработку запроса
    }
}

