package com.miners.shop.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Утилита для копирования ресурсов шаблона при старте приложения
 */
public class AssetsCopyUtil {
    
    private static final Logger log = LoggerFactory.getLogger(AssetsCopyUtil.class);
    
    // Путь к ресурсам - bootstrapTheme находится внутри shop-backend/src/main/resources/
    private static final String SOURCE_BASE = "src/main/resources/bootstrapTheme/MarketPro/marketpro/assets";
    private static final String DEST_BASE = "src/main/resources/static/assets";
    
    /**
     * Копирует ресурсы шаблона в static/assets при старте приложения
     */
    public static void copyAssetsIfNeeded() {
        try {
            // Определяем корневую директорию проекта
            // bootstrapTheme находится внутри shop-backend/src/main/resources/
            // поэтому рабочая директория - это shop-backend
            File currentDir = new File(".");
            String currentPath = currentDir.getCanonicalPath();
            log.info("📂 Текущая рабочая директория: {}", currentPath);
            
            // Если мы в shop-backend, используем текущую директорию как базовую
            File projectRoot = currentDir;
            if (currentPath.contains("shop-backend")) {
                // Проверяем, что bootstrapTheme существует относительно текущей директории
                File bootstrapThemeCheck = new File(currentDir, SOURCE_BASE);
                if (bootstrapThemeCheck.exists()) {
                    log.info("✅ BootstrapTheme найден относительно текущей директории: {}", bootstrapThemeCheck.getCanonicalPath());
                    projectRoot = currentDir;
                } else {
                    // Пробуем найти корень проекта (где есть bootstrapTheme в корне)
                    projectRoot = findProjectRoot(currentDir);
                }
            } else {
                // Пробуем найти корень проекта (где есть bootstrapTheme)
                projectRoot = findProjectRoot(currentDir);
            }
            if (projectRoot == null) {
                log.error("❌ Не удалось найти корень проекта с bootstrapTheme");
                log.error("   Текущая директория: {}", currentPath);
                log.error("   Пробуем альтернативный способ...");
                
                // Альтернативный способ: ищем через classpath
                try {
                    String classPath = System.getProperty("java.class.path");
                    if (classPath != null && classPath.contains("shop-backend")) {
                        // Извлекаем путь к shop-backend из classpath
                        String[] paths = classPath.split(File.pathSeparator);
                        for (String cp : paths) {
                            if (cp.contains("shop-backend") && cp.contains("target")) {
                                File cpFile = new File(cp);
                                File shopBackend = cpFile.getParentFile().getParentFile();
                                File altRoot = shopBackend.getParentFile();
                                if (altRoot != null) {
                                    File bootstrapTheme = new File(altRoot, "bootstrapTheme");
                                    if (bootstrapTheme.exists()) {
                                        projectRoot = altRoot;
                                        log.info("📁 Корень проекта найден через classpath: {}", projectRoot.getCanonicalPath());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("Ошибка при поиске через classpath: {}", e.getMessage());
                }
                
                if (projectRoot == null) {
                    log.error("❌ Не удалось найти корень проекта. Ресурсы не будут скопированы.");
                    log.error("   Вручную скопируйте: {} -> {}", SOURCE_BASE, DEST_BASE);
                    return;
                }
            }
            
            String projectRootPath = projectRoot.getCanonicalPath();
            log.info("📁 Корень проекта: {}", projectRootPath);
            
            Path sourcePath = Paths.get(projectRootPath, SOURCE_BASE.split("/"));
            Path destPath = Paths.get(projectRootPath, DEST_BASE.split("/"));
            
            log.info("   📦 Источник: {}", sourcePath);
            log.info("   📥 Назначение: {}", destPath);
            
            if (!Files.exists(sourcePath)) {
                log.warn("⚠️  Исходная папка с ресурсами не найдена: {}", sourcePath);
                return;
            }
            
            // Проверяем, скопированы ли уже ресурсы (проверяем наличие хотя бы одного файла CSS или JS)
            boolean assetsExist = false;
            if (Files.exists(destPath)) {
                try (var stream = Files.walk(destPath)) {
                    long fileCount = stream.filter(Files::isRegularFile)
                            .filter(p -> {
                                String pathStr = p.toString().toLowerCase();
                                return pathStr.endsWith(".css") || pathStr.endsWith(".js") || pathStr.endsWith(".png");
                            })
                            .limit(1)
                            .count();
                    if (fileCount > 0) {
                        assetsExist = true;
                        log.debug("Найдено файлов в папке назначения: {}", fileCount);
                    }
                } catch (Exception e) {
                    log.debug("Ошибка при проверке существующих ресурсов: {}", e.getMessage());
                }
            }
            
            // Для принудительного копирования проверяем флаг
            boolean forceCopy = Boolean.parseBoolean(System.getProperty("assets.force.copy", "false"));
            
            if (assetsExist && !forceCopy) {
                log.info("✅ Ресурсы уже скопированы в: {}", destPath);
                log.info("   Для принудительного копирования установите системное свойство: -Dassets.force.copy=true");
                return;
            }
            
            if (forceCopy && assetsExist) {
                log.info("🔄 Принудительное копирование (ресурсы будут перезаписаны)");
            }
            
            log.info("📦 Начинаю копирование ресурсов шаблона...");
            log.info("   Из: {}", sourcePath);
            log.info("   В: {}", destPath);
            
            // Создаем целевую директорию
            Files.createDirectories(destPath);
            
            // Копируем рекурсивно
            log.info("🔄 Копирование файлов...");
            long filesCopied = copyRecursive(sourcePath, destPath);
            
            log.info("✅ Ресурсы успешно скопированы! Скопировано файлов: {}", filesCopied);
            
        } catch (Exception e) {
            log.error("❌ Ошибка при копировании ресурсов: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Находит корень проекта (директорию с bootstrapTheme)
     */
    private static File findProjectRoot(File startDir) {
        try {
            // Способ 1: Если запускаем из shop-backend, сразу идем на уровень выше
            String canonicalPath = startDir.getCanonicalPath();
            log.info("🔍 Поиск корня проекта. Начальная директория: {}", canonicalPath);
            
            if (canonicalPath.contains("shop-backend")) {
                File checkDir = null;
                
                // Способ 1: Если путь заканчивается на shop-backend, берем родителя через строку
                if (canonicalPath.endsWith("shop-backend") || canonicalPath.endsWith("shop-backend\\") || canonicalPath.endsWith("shop-backend/")) {
                    // Извлекаем путь до shop-backend из строки
                    int shopBackendIndex = canonicalPath.lastIndexOf("shop-backend");
                    if (shopBackendIndex > 0) {
                        String rootPath = canonicalPath.substring(0, shopBackendIndex - 1);
                        checkDir = new File(rootPath);
                        log.info("🔍 Путь заканчивается на shop-backend, извлекаю корень из строки: {}", checkDir.getCanonicalPath());
                    }
                }
                
                // Способ 2: Если текущая директория - shop-backend, берем родителя
                if (checkDir == null && startDir.getName().equals("shop-backend")) {
                    File parent = startDir.getParentFile();
                    if (parent != null && parent.exists()) {
                        checkDir = parent;
                        log.info("🔍 Текущая директория - shop-backend, проверяю родителя: {}", checkDir.getCanonicalPath());
                    }
                }
                
                // Способ 3: Если shop-backend в середине пути, извлекаем путь до него
                if (checkDir == null) {
                    int shopBackendIndex = canonicalPath.indexOf("shop-backend");
                    if (shopBackendIndex > 0) {
                        String rootPath = canonicalPath.substring(0, shopBackendIndex - 1);
                        checkDir = new File(rootPath);
                        log.info("🔍 shop-backend в середине пути, извлекаю корень: {}", checkDir.getCanonicalPath());
                    }
                }
                
                if (checkDir == null) {
                    log.warn("🔍 Не удалось определить корень из пути: {}", canonicalPath);
                } else {
                    File bootstrapTheme = new File(checkDir, "bootstrapTheme");
                    log.info("🔍 Проверяю корень проекта: {}", checkDir.getCanonicalPath());
                    log.info("🔍 BootstrapTheme путь: {}", bootstrapTheme.getAbsolutePath());
                    log.info("🔍 BootstrapTheme существует: {}", bootstrapTheme.exists());
                    
                    if (bootstrapTheme.exists() && bootstrapTheme.isDirectory()) {
                        log.info("✅ Найден корень проекта: {}", checkDir.getCanonicalPath());
                        return checkDir;
                    }
                }
            }
            
            // Способ 2: Поднимаемся по дереву директорий
            File current = startDir;
            int maxDepth = 10;
            int depth = 0;
            
            while (current != null && depth < maxDepth) {
                File bootstrapTheme = new File(current, "bootstrapTheme");
                if (bootstrapTheme.exists() && bootstrapTheme.isDirectory()) {
                    log.debug("✅ Найден корень проекта: {}", current.getCanonicalPath());
                    return current;
                }
                
                // Также проверяем родительскую директорию
                File parent = current.getParentFile();
                if (parent != null) {
                    bootstrapTheme = new File(parent, "bootstrapTheme");
                    if (bootstrapTheme.exists() && bootstrapTheme.isDirectory()) {
                        log.debug("✅ Найден корень проекта (родитель): {}", parent.getCanonicalPath());
                        return parent;
                    }
                }
                
                current = parent;
                depth++;
            }
            
            // Способ 3: Относительный путь ".." от текущей директории
            try {
                File relativeRoot = new File(startDir, "..").getCanonicalFile();
                File bootstrapTheme = new File(relativeRoot, "bootstrapTheme");
                if (bootstrapTheme.exists() && bootstrapTheme.isDirectory()) {
                    log.debug("✅ Найден корень через относительный путь: {}", relativeRoot.getCanonicalPath());
                    return relativeRoot;
                }
            } catch (Exception e) {
                log.debug("Ошибка при проверке относительного пути: {}", e.getMessage());
            }
            
            // Способ 4: Пробуем найти через системные свойства
            String userDir = System.getProperty("user.dir");
            if (userDir != null && userDir.contains("shop-backend")) {
                int index = userDir.indexOf("shop-backend");
                if (index > 0) {
                    String rootPath = userDir.substring(0, index - 1);
                    File rootCandidate = new File(rootPath);
                    File bootstrapTheme = new File(rootCandidate, "bootstrapTheme");
                    if (bootstrapTheme.exists() && bootstrapTheme.isDirectory()) {
                        log.debug("✅ Найден корень через user.dir: {}", rootCandidate.getCanonicalPath());
                        return rootCandidate;
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Ошибка при поиске корня проекта: {}", e.getMessage(), e);
        }
        
        return null;
    }
    
    private static long copyRecursive(Path source, Path dest) throws IOException {
        if (Files.isDirectory(source)) {
            Files.createDirectories(dest);
            long count = 0;
            try (var stream = Files.list(source)) {
                for (Path child : stream.toList()) {
                    try {
                        count += copyRecursive(child, dest.resolve(child.getFileName()));
                    } catch (IOException e) {
                        log.error("Ошибка при копировании {}: {}", child, e.getMessage());
                    }
                }
            }
            return count;
        } else {
            Files.createDirectories(dest.getParent());
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
            return 1;
        }
    }
}

