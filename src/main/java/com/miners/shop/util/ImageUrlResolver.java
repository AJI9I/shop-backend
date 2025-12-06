package com.miners.shop.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Утилита для поиска изображений товаров в папке /img/
 * На основе модели майнера генерирует URL к изображению
 */
@Component
@Slf4j
public class ImageUrlResolver {
    
    private static final String IMG_DIR = "classpath:/img/";
    private static final List<String> IMAGE_EXTENSIONS = Arrays.asList(".webp", ".jpg", ".jpeg", ".png", ".gif");
    
    private final ResourceLoader resourceLoader;
    
    public ImageUrlResolver(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
    
    /**
     * Разрешает URL изображения для модели майнера
     * Ищет файл в папке /img/ на основе нормализованного названия модели
     * 
     * @param model Модель майнера (например: "S21 HYD", "Antminer S19")
     * @return URL изображения или null, если не найдено
     */
    public String resolveImageUrl(String model) {
        if (model == null || model.trim().isEmpty()) {
            log.debug("Модель пуста, возвращаем null");
            return null;
        }
        
        log.debug("Поиск изображения для модели: '{}'", model);
        
        // Сначала пробуем точное совпадение с разными вариантами регистра
        // Для файла "Antminer-S21-Hyd.webp" пробуем разные варианты
        String[] variants = {
            capitalizeModelName(model), // "Antminer-S21-Hyd" - главный вариант для файла "Antminer-S21-Hyd.webp"
            normalizeModelName(model), // "antminer-s21-hyd"
            model.toLowerCase().replaceAll("[^a-z0-9\\s]+", "").replaceAll("\\s+", "-").replaceAll("-+", "-").replaceAll("^-+|-+$", "") // "s21-hyd"
        };
        
        log.debug("Варианты для поиска: {}", Arrays.toString(variants));
        
        for (String variant : variants) {
            for (String ext : IMAGE_EXTENSIONS) {
                String fileName = variant + ext;
                log.debug("Проверяем файл: {}", fileName);
                
                // Проверяем существование файла
                if (fileExists(fileName)) {
                    String url = "/img/" + fileName;
                    log.info("✅ Найдено изображение для модели '{}': {}", model, url);
                    return url;
                }
            }
        }
        
        // Если точного совпадения нет, пробуем найти частичное совпадение
        String partialMatch = findPartialMatch(model);
        if (partialMatch != null) {
            log.info("✅ Найдено частичное совпадение изображения для модели '{}': {}", model, partialMatch);
            return partialMatch;
        }
        
        // Если конкретное изображение не найдено, используем общее изображение-заглушку
        // Сначала пробуем найти любое изображение в папке /img/ как заглушку
        String fallbackImage = findAnyImageInFolder();
        if (fallbackImage != null) {
            log.info("⚠️  Изображение для модели '{}' не найдено, используем заглушку: {}", model, fallbackImage);
            return fallbackImage;
        }
        
        // Если даже заглушки нет, возвращаем null - шаблон использует placeholder из темы
        log.info("⚠️  Изображение для модели '{}' не найдено, возвращаем null (будет использован placeholder из темы)", model);
        return null;
    }
    
    /**
     * Преобразует название модели в формат с заглавными буквами
     * Примеры:
     * - "S21 HYD" -> "Antminer-S21-Hyd"
     * - "Antminer S19" -> "Antminer-S19"
     */
    private String capitalizeModelName(String model) {
        String normalized = model.toLowerCase()
                .replaceAll("[^a-z0-9\\s]+", "") // Убираем специальные символы
                .replaceAll("\\s+", "-") // Заменяем пробелы на дефисы
                .replaceAll("-+", "-") // Убираем повторяющиеся дефисы
                .replaceAll("^-+|-+$", ""); // Убираем дефисы в начале и конце
        
        // Добавляем префикс "antminer" если нет
        if (!normalized.startsWith("antminer")) {
            normalized = "antminer-" + normalized;
        }
        
        // Преобразуем в формат с заглавными буквами (каждое слово с заглавной)
        String[] parts = normalized.split("-");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                result.append("-");
            }
            if (!parts[i].isEmpty()) {
                result.append(Character.toUpperCase(parts[i].charAt(0)));
                if (parts[i].length() > 1) {
                    result.append(parts[i].substring(1));
                }
            }
        }
        
        return result.toString();
    }
    
    /**
     * Нормализует название модели для поиска файла
     * Примеры:
     * - "S21 HYD" -> "antminer-s21-hyd"
     * - "Antminer S19" -> "antminer-s19"
     * - "S21 HYD 200T" -> "antminer-s21-hyd-200t"
     * - "S21" -> "antminer-s21"
     * 
     * Также проверяет точные совпадения с файлами в папке img/
     */
    private String normalizeModelName(String model) {
        String normalized = model.toLowerCase()
                .replaceAll("[^a-z0-9\\s]+", "") // Убираем специальные символы
                .replaceAll("\\s+", "-") // Заменяем пробелы на дефисы
                .replaceAll("-+", "-") // Убираем повторяющиеся дефисы
                .replaceAll("^-+|-+$", ""); // Убираем дефисы в начале и конце
        
        // Если модель не начинается с "antminer", добавляем префикс
        // Для совместимости с файлом "Antminer-S21-Hyd.webp"
        if (!normalized.startsWith("antminer")) {
            normalized = "antminer-" + normalized;
        }
        
        return normalized;
    }
    
    /**
     * Проверяет существование файла в папке /img/
     */
    private boolean fileExists(String fileName) {
        try {
            Resource resource = resourceLoader.getResource(IMG_DIR + fileName);
            return resource.exists() && resource.isReadable();
        } catch (Exception e) {
            log.debug("Ошибка при проверке существования файла {}: {}", fileName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Ищет частичное совпадение в названиях файлов
     * Например, если модель "S21 HYD 200T", а файл "antminer-s21-hyd.webp"
     * Также проверяет файлы без префикса "antminer"
     */
    private String findPartialMatch(String model) {
        String normalized = normalizeModelName(model);
        String[] parts = normalized.split("-");
        
        // Убираем "antminer" из начала, если есть
        if (parts.length > 0 && parts[0].equals("antminer")) {
            parts = Arrays.copyOfRange(parts, 1, parts.length);
        }
        
        // Пробуем разные комбинации частей модели
        for (int i = parts.length; i > 0; i--) {
            String partialName = String.join("-", Arrays.copyOf(parts, i));
            
            // Пробуем с префиксом antminer
            for (String ext : IMAGE_EXTENSIONS) {
                String fileName = "antminer-" + partialName + ext;
                if (fileExists(fileName)) {
                    return "/img/" + fileName;
                }
            }
            
            // Пробуем без префикса antminer
            for (String ext : IMAGE_EXTENSIONS) {
                String fileName = partialName + ext;
                if (fileExists(fileName)) {
                    return "/img/" + fileName;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Ищет любое изображение в папке /img/ для использования в качестве заглушки
     * Возвращает первое найденное изображение
     */
    private String findAnyImageInFolder() {
        // Пробуем найти известные файлы-заглушки
        String[] fallbackFiles = {
            "Antminer-S21-Hyd.webp",
            "antminer-s21-hyd.webp",
            "c846c7ed-8220-48b6-9af2-39d131795854_540.jpg"
        };
        
        for (String fileName : fallbackFiles) {
            if (fileExists(fileName)) {
                return "/img/" + fileName;
            }
        }
        
        // Если известные файлы не найдены, пробуем найти любое изображение
        // Проходим по всем расширениям и проверяем файлы
        for (String ext : IMAGE_EXTENSIONS) {
            // Пробуем разные варианты названий
            String[] testFiles = {
                "Antminer-S21-Hyd" + ext,
                "antminer-s21-hyd" + ext,
                "Antminer" + ext
            };
            
            for (String testFile : testFiles) {
                if (fileExists(testFile)) {
                    return "/img/" + testFile;
                }
            }
        }
        
        return null;
    }
}
