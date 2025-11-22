package com.miners.shop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Конфигурация для раздачи статических ресурсов (изображений)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Раздаем изображения из resources/img
        registry.addResourceHandler("/img/**")
                .addResourceLocations("classpath:/img/");
        
        // Раздаем ресурсы шаблона MarketPro из resources/static/assets
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/");
    }
}

