package com.miners.shop.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * Конфигурация для установки часового пояса приложения на Москву
 */
@Configuration
public class TimeZoneConfig {
    
    @PostConstruct
    public void init() {
        // Устанавливаем часовой пояс по умолчанию для всего приложения
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Moscow"));
        System.setProperty("user.timezone", "Europe/Moscow");
    }
}

