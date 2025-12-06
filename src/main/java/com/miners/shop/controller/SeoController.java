package com.miners.shop.controller;

import com.miners.shop.entity.MinerDetail;
import com.miners.shop.repository.MinerDetailRepository;
import com.miners.shop.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Контроллер для обработки SEO файлов (robots.txt, sitemap.xml)
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class SeoController {

    private final MinerDetailRepository minerDetailRepository;
    
    private static final String BASE_URL = "https://minerhive.ru";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Обработка запроса robots.txt
     */
    @GetMapping("/robots.txt")
    public ResponseEntity<String> robotsTxt() {
        try {
            Resource resource = new ClassPathResource("static/robots.txt");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentLength(content.getBytes(StandardCharsets.UTF_8).length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IOException e) {
            log.error("Ошибка при чтении robots.txt: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Динамическая генерация sitemap.xml с ЧПУ ссылками
     * Автоматически генерирует slug для товаров, у которых его нет
     */
    @GetMapping("/sitemap.xml")
    @Transactional
    public ResponseEntity<String> sitemapXml() {
        try {
            StringBuilder sitemap = new StringBuilder();
            sitemap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            sitemap.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
            
            String currentDate = LocalDateTime.now().format(DATE_FORMATTER);
            
            // Главная страница
            addUrl(sitemap, BASE_URL + "/", currentDate, "daily", "1.0");
            
            // Страница каталога товаров
            addUrl(sitemap, BASE_URL + "/products", currentDate, "daily", "0.8");
            
            // Получаем все MinerDetail
            List<MinerDetail> minerDetails = minerDetailRepository.findAll();
            int generatedSlugsCount = 0;
            
            for (MinerDetail minerDetail : minerDetails) {
                // Пропускаем неактивные товары
                if (minerDetail.getActive() != null && !minerDetail.getActive()) {
                    continue;
                }
                
                String slug = minerDetail.getSlug();
                
                // Если slug отсутствует, генерируем его
                if (slug == null || slug.isEmpty()) {
                    if (minerDetail.getStandardName() != null && !minerDetail.getStandardName().isEmpty()) {
                        slug = SlugUtil.generateSlug(minerDetail.getStandardName());
                        
                        // Проверяем уникальность slug
                        String finalSlug = slug;
                        int counter = 1;
                        while (minerDetailRepository.existsBySlug(finalSlug)) {
                            finalSlug = slug + "-" + counter;
                            counter++;
                        }
                        slug = finalSlug;
                        
                        // Сохраняем сгенерированный slug
                        minerDetail.setSlug(slug);
                        minerDetailRepository.save(minerDetail);
                        generatedSlugsCount++;
                        log.debug("Сгенерирован slug для товара {}: {}", minerDetail.getId(), slug);
                    } else {
                        // Если нет названия, используем ID
                        slug = String.valueOf(minerDetail.getId());
                    }
                }
                
                // Используем ЧПУ ссылку
                String url = BASE_URL + "/products/" + slug;
                addUrl(sitemap, url, currentDate, "weekly", "0.7");
            }
            
            sitemap.append("</urlset>");
            
            String content = sitemap.toString();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.setContentLength(content.getBytes(StandardCharsets.UTF_8).length);
            
            log.info("Сгенерирован sitemap.xml с {} товарами (создано {} новых slug)", 
                    minerDetails.size(), generatedSlugsCount);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (Exception e) {
            log.error("Ошибка при генерации sitemap.xml: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * Добавление URL в sitemap
     */
    private void addUrl(StringBuilder sitemap, String loc, String lastmod, String changefreq, String priority) {
        sitemap.append("  <url>\n");
        sitemap.append("    <loc>").append(escapeXml(loc)).append("</loc>\n");
        sitemap.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        sitemap.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        sitemap.append("    <priority>").append(priority).append("</priority>\n");
        sitemap.append("  </url>\n");
    }
    
    /**
     * Экранирование XML символов
     */
    private String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}
