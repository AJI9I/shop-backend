package com.miners.shop.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Конфигурация для раздачи статических ресурсов (изображений)
 */
@Configuration
@Order(Ordered.LOWEST_PRECEDENCE)
@Slf4j
public class WebConfig implements WebMvcConfigurer {
    
    @Value("${app.upload.dir:uploads/img/miner-details}")
    private String uploadDir;
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // ВАЖНО: Устанавливаем низкий приоритет для статических ресурсов,
        // чтобы контроллеры обрабатывались первыми
        registry.setOrder(Ordered.LOWEST_PRECEDENCE);
        
        // Раздаем изображения из resources/img (статические файлы, компилируются в JAR)
        // Кэширование на 1 год для статических ресурсов
        registry.addResourceHandler("/img/**")
                .addResourceLocations("classpath:/img/", "classpath:/static/img/")
                .setCachePeriod(31536000)  // 1 год в секундах
                .resourceChain(true);  // Включить цепочку ресурсов для оптимизации
        
        // Добавляем путь для загружаемых файлов (динамически загруженные изображения)
        // Определяем абсолютный путь к директории загрузки
        try {
            Path uploadPath = Paths.get(uploadDir);
            String uploadLocation;
            
            if (uploadPath.isAbsolute()) {
                // Абсолютный путь
                uploadLocation = "file:" + uploadPath.toAbsolutePath().toString().replace("\\", "/");
                if (!uploadLocation.endsWith("/")) {
                    uploadLocation += "/";
                }
            } else {
                // Относительный путь - разрешаем относительно корня проекта
                Path currentPath = Paths.get("").toAbsolutePath();
                Path projectRoot = currentPath;
                
                // Ищем корень проекта
                while (projectRoot != null && !java.nio.file.Files.exists(projectRoot.resolve("pom.xml")) 
                        && !projectRoot.getFileName().toString().equals("shop-backend")) {
                    Path parent = projectRoot.getParent();
                    if (parent == null || parent.equals(projectRoot)) {
                        break;
                    }
                    projectRoot = parent;
                }
                
                if (!java.nio.file.Files.exists(projectRoot.resolve("pom.xml")) 
                        && !projectRoot.getFileName().toString().equals("shop-backend")) {
                    if (java.nio.file.Files.exists(currentPath.resolve("shop-backend"))) {
                        projectRoot = currentPath.resolve("shop-backend");
                    } else if (java.nio.file.Files.exists(currentPath.resolve("pom.xml"))) {
                        projectRoot = currentPath;
                    }
                }
                
                Path resolvedPath = projectRoot.resolve(uploadDir).toAbsolutePath();
                uploadLocation = "file:" + resolvedPath.toString().replace("\\", "/");
                if (!uploadLocation.endsWith("/")) {
                    uploadLocation += "/";
                }
            }
            
            // Добавляем подпапку miner-details к пути
            String minerDetailsLocation = uploadLocation;
            if (!minerDetailsLocation.endsWith("miner-details/")) {
                if (!minerDetailsLocation.endsWith("/")) {
                    minerDetailsLocation += "/";
                }
                minerDetailsLocation += "miner-details/";
            }
            
            log.info("Настроен путь для загружаемых изображений: {}", minerDetailsLocation);
            // Кэширование на 1 час для динамически загруженных изображений
            registry.addResourceHandler("/img/miner-details/**")
                    .addResourceLocations(minerDetailsLocation)
                    .setCachePeriod(3600)  // 1 час в секундах
                    .resourceChain(true);
        } catch (Exception e) {
            log.warn("Не удалось настроить путь для загружаемых изображений, используется fallback: {}", e.getMessage());
            // Fallback на путь по умолчанию
            registry.addResourceHandler("/img/miner-details/**")
                    .addResourceLocations("file:C:/IdeaPRG/miners/siteImg/miner-details/")
                    .setCachePeriod(3600)
                    .resourceChain(true);
        }
        
        // Раздаем ресурсы шаблона MarketPro из resources/static/assets
        // Кэширование на 1 год для статических ресурсов
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCachePeriod(31536000)  // 1 год
                .resourceChain(true);
        
        // Раздаем ресурсы шаблона BootstrapTheme HTML из resources/bootstrapTheme/HTML/dist/
        // Путь в HTML: /bootstrap-theme/assets/... -> classpath:/bootstrapTheme/HTML/dist/assets/...
        // Кэширование на 1 год для статических ресурсов
        registry.addResourceHandler("/bootstrap-theme/**")
                .addResourceLocations("classpath:/bootstrapTheme/HTML/dist/")
                .setCachePeriod(31536000)  // 1 год
                .resourceChain(true);
    }
    
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Устанавливаем низкий приоритет для статических ресурсов
        // чтобы контроллеры обрабатывались первыми
        registry.setOrder(Ordered.LOWEST_PRECEDENCE);
    }
}

