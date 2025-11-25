package com.miners.shop.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Сервис для загрузки и сохранения изображений
 */
@Service
@Slf4j
public class ImageUploadService {
    
    @Value("${app.upload.dir:uploads/img/miner-details}")
    private String uploadDir;
    
    /**
     * Получает абсолютный путь к директории загрузки
     * Поддерживает как абсолютные, так и относительные пути
     * 
     * Если путь абсолютный (начинается с / или C:\) - используется как есть
     * Если путь относительный - разрешается относительно корня проекта
     * 
     * Файлы сохраняются вне src/main/resources, чтобы не компилировались в JAR
     */
    private Path getUploadPath() {
        try {
            Path uploadPath = Paths.get(uploadDir);
            
            // Если путь абсолютный, используем его как есть
            if (uploadPath.isAbsolute()) {
                log.info("Используется абсолютный путь для загрузки изображений: {}", uploadPath);
                return uploadPath;
            }
            
            // Если путь относительный, разрешаем его относительно корня проекта
            Path currentPath = Paths.get("").toAbsolutePath();
            Path projectRoot = currentPath;
            
            // Ищем корень проекта (где находится pom.xml или shop-backend)
            while (projectRoot != null && !Files.exists(projectRoot.resolve("pom.xml")) 
                    && !projectRoot.getFileName().toString().equals("shop-backend")) {
                Path parent = projectRoot.getParent();
                if (parent == null || parent.equals(projectRoot)) {
                    break;
                }
                projectRoot = parent;
            }
            
            // Если не нашли shop-backend, используем текущую директорию
            if (!Files.exists(projectRoot.resolve("pom.xml")) && !projectRoot.getFileName().toString().equals("shop-backend")) {
                // Пробуем найти shop-backend в текущей директории
                if (Files.exists(currentPath.resolve("shop-backend"))) {
                    projectRoot = currentPath.resolve("shop-backend");
                } else if (Files.exists(currentPath.resolve("pom.xml"))) {
                    projectRoot = currentPath;
                } else {
                    projectRoot = currentPath;
                }
            }
            
            Path resolvedPath = projectRoot.resolve(uploadDir).toAbsolutePath();
            log.info("Путь для загрузки изображений (относительный, разрешен относительно корня проекта): {}", resolvedPath);
            return resolvedPath;
        } catch (Exception e) {
            log.error("Ошибка при определении пути загрузки: {}", e.getMessage(), e);
            // В случае ошибки пробуем использовать путь как абсолютный
            try {
                Path fallbackPath = Paths.get(uploadDir).toAbsolutePath();
                log.warn("Используется fallback путь: {}", fallbackPath);
                return fallbackPath;
            } catch (Exception e2) {
                log.error("Критическая ошибка: не удалось определить путь для загрузки файлов", e2);
                throw new RuntimeException("Не удалось определить путь для загрузки файлов: " + uploadDir, e2);
            }
        }
    }
    
    /**
     * Сохраняет загруженное изображение и возвращает URL для доступа к нему
     * 
     * @param file Загруженный файл
     * @param minerDetailId ID майнера (для именования файла)
     * @return URL изображения (например: /img/miner-details/123-abc.jpg) или null при ошибке
     */
    public String saveImage(MultipartFile file, Long minerDetailId) {
        if (file == null || file.isEmpty()) {
            log.debug("Файл не предоставлен или пуст");
            return null;
        }
        
        // Проверяем тип файла
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            log.warn("Загруженный файл не является изображением: {}", contentType);
            return null;
        }
        
        try {
            // Определяем расширение файла
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            } else {
                // Определяем расширение по MIME типу
                switch (contentType) {
                    case "image/jpeg":
                    case "image/jpg":
                        extension = ".jpg";
                        break;
                    case "image/png":
                        extension = ".png";
                        break;
                    case "image/gif":
                        extension = ".gif";
                        break;
                    case "image/webp":
                        extension = ".webp";
                        break;
                    default:
                        extension = ".jpg";
                        break;
                }
            }
            
            // Генерируем уникальное имя файла
            String fileName = minerDetailId + "-" + UUID.randomUUID().toString().substring(0, 8) + extension;
            
            // Получаем базовый путь для сохранения (из конфигурации)
            Path baseUploadPath = getUploadPath();
            
            // Создаем подпапку miner-details внутри базовой директории
            Path uploadPath = baseUploadPath.resolve("miner-details");
            
            // Создаем директорию, если её нет
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Создана директория для загрузки изображений: {}", uploadPath.toAbsolutePath());
            }
            
            // Сохраняем файл
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Формируем URL для доступа к файлу
            // URL будет /img/miner-details/filename
            String url = "/img/miner-details/" + fileName;
            
            log.info("Изображение успешно сохранено: {} -> {}", file.getOriginalFilename(), url);
            return url;
            
        } catch (IOException e) {
            log.error("Ошибка при сохранении изображения: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Удаляет изображение по URL
     * 
     * @param imageUrl URL изображения (например: /img/miner-details/123-abc.jpg)
     */
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }
        
        try {
            // Извлекаем имя файла из URL
            String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            Path baseUploadPath = getUploadPath();
            // Файлы хранятся в подпапке miner-details
            Path uploadPath = baseUploadPath.resolve("miner-details");
            Path filePath = uploadPath.resolve(fileName);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Изображение удалено: {}", filePath);
            } else {
                log.debug("Файл не найден для удаления: {}", filePath);
            }
        } catch (IOException e) {
            log.error("Ошибка при удалении изображения {}: {}", imageUrl, e.getMessage(), e);
        }
    }
}

