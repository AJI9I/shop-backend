package com.miners.shop;

import com.miners.shop.config.AssetsCopyUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ShopApplication {
    public static void main(String[] args) {
        // Копируем ресурсы шаблона при старте приложения
        AssetsCopyUtil.copyAssetsIfNeeded();
        
        SpringApplication.run(ShopApplication.class, args);
    }
}
