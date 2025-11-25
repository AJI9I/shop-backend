package com.miners.shop.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация кэширования для приложения
 * Использует встроенный ConcurrentMapCacheManager для кэширования данных
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {
    
    /**
     * Настройка менеджера кэша
     * Использует ConcurrentMapCacheManager для кэширования данных
     * Кэш хранится в памяти и очищается при перезапуске приложения
     */
    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager("cryptoData", "minerDetails");
        cacheManager.setAllowNullValues(false);
        
        log.info("Кэширование настроено: ConcurrentMapCacheManager для cryptoData и minerDetails");
        return cacheManager;
    }
}

