package com.miners.shop.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Утилита для SEO функций
 */
public class SeoUtil {
    
    private static final String BASE_URL = "https://minerhive.ru";
    
    /**
     * Генерирует canonical URL для текущего запроса
     * 
     * @param request HTTP запрос
     * @return Canonical URL
     */
    public static String generateCanonicalUrl(HttpServletRequest request) {
        String requestUrl = request.getRequestURL().toString();
        String queryString = request.getQueryString();
        
        // Убираем query параметры для canonical URL (кроме необходимых)
        if (queryString != null && !queryString.isEmpty()) {
            // Можно оставить некоторые параметры, но обычно canonical без параметров
            // requestUrl += "?" + queryString;
        }
        
        return requestUrl;
    }
    
    /**
     * Генерирует canonical URL для указанного пути
     * 
     * @param path Путь (например: "/products/antminer-s19j-pro")
     * @return Canonical URL
     */
    public static String generateCanonicalUrl(String path) {
        if (path == null || path.isEmpty()) {
            return BASE_URL;
        }
        
        // Убираем начальный слэш, если есть
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        
        return BASE_URL + "/" + path;
    }
    
    /**
     * Получить базовый URL сайта
     */
    public static String getBaseUrl() {
        return BASE_URL;
    }
}





