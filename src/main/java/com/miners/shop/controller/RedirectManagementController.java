package com.miners.shop.controller;

import com.miners.shop.entity.NotFoundError;
import com.miners.shop.entity.Redirect;
import com.miners.shop.repository.NotFoundErrorRepository;
import com.miners.shop.repository.RedirectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Контроллер для управления редиректами и просмотра 404 ошибок
 * Доступен только для администраторов
 */
@Controller
@RequestMapping("/private/redirects")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class RedirectManagementController {
    
    private final RedirectRepository redirectRepository;
    private final NotFoundErrorRepository notFoundErrorRepository;
    
    /**
     * Страница со списком редиректов
     */
    @GetMapping
    @Transactional(readOnly = true)
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDir,
            Model model) {
        
        Sort.Direction direction = sortDir.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Redirect> redirectsPage = redirectRepository.findAll(pageable);
        
        model.addAttribute("redirectsPage", redirectsPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        
        return "redirects/list";
    }
    
    /**
     * Страница со списком 404 ошибок
     */
    @GetMapping("/404-errors")
    @Transactional(readOnly = true)
    public String notFoundErrors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Model model) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastOccurred"));
        Page<NotFoundError> errorsPage = notFoundErrorRepository.findAllByOrderByLastOccurredDesc(pageable);
        
        model.addAttribute("errorsPage", errorsPage);
        model.addAttribute("currentPage", page);
        
        return "redirects/404-errors";
    }
    
    /**
     * API для создания редиректа
     */
    @PostMapping("/api/create")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> createRedirect(@RequestBody Map<String, Object> request) {
        try {
            String fromUrl = (String) request.get("fromUrl");
            String toUrl = (String) request.get("toUrl");
            Integer redirectType = request.get("redirectType") != null 
                    ? Integer.parseInt(request.get("redirectType").toString()) 
                    : 301;
            
            if (fromUrl == null || fromUrl.trim().isEmpty() || 
                toUrl == null || toUrl.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "URL обязательны");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Проверяем, не существует ли уже редирект для этого URL
            if (redirectRepository.existsByFromUrl(fromUrl.trim())) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Редирект для этого URL уже существует");
                return ResponseEntity.badRequest().body(error);
            }
            
            Redirect redirect = new Redirect();
            redirect.setFromUrl(fromUrl.trim());
            redirect.setToUrl(toUrl.trim());
            redirect.setRedirectType(redirectType);
            redirect.setActive(true);
            
            redirectRepository.save(redirect);
            
            log.info("Создан редирект: {} -> {} (тип: {})", fromUrl, toUrl, redirectType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Редирект успешно создан");
            response.put("redirect", Map.of(
                "id", redirect.getId(),
                "fromUrl", redirect.getFromUrl(),
                "toUrl", redirect.getToUrl(),
                "redirectType", redirect.getRedirectType()
            ));
            
            return ResponseEntity.ok().body(response);
            
        } catch (Exception e) {
            log.error("Ошибка при создании редиректа: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Ошибка при создании редиректа: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * API для обновления редиректа
     */
    @PostMapping("/api/update/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> updateRedirect(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            Optional<Redirect> redirectOpt = redirectRepository.findById(id);
            if (redirectOpt.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Редирект не найден");
                return ResponseEntity.status(404).body(error);
            }
            
            Redirect redirect = redirectOpt.get();
            
            String toUrl = (String) request.get("toUrl");
            Integer redirectType = request.get("redirectType") != null 
                    ? Integer.parseInt(request.get("redirectType").toString()) 
                    : null;
            Boolean active = request.get("active") != null 
                    ? Boolean.parseBoolean(request.get("active").toString()) 
                    : null;
            
            if (toUrl != null && !toUrl.trim().isEmpty()) {
                redirect.setToUrl(toUrl.trim());
            }
            if (redirectType != null) {
                redirect.setRedirectType(redirectType);
            }
            if (active != null) {
                redirect.setActive(active);
            }
            
            redirectRepository.save(redirect);
            
            log.info("Обновлен редирект ID={}: toUrl={}, redirectType={}, active={}", 
                    id, redirect.getToUrl(), redirect.getRedirectType(), redirect.getActive());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Редирект успешно обновлен");
            
            return ResponseEntity.ok().body(response);
            
        } catch (Exception e) {
            log.error("Ошибка при обновлении редиректа: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Ошибка при обновлении редиректа: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * API для удаления редиректа
     */
    @PostMapping("/api/delete/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteRedirect(@PathVariable Long id) {
        try {
            Optional<Redirect> redirectOpt = redirectRepository.findById(id);
            if (redirectOpt.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Редирект не найден");
                return ResponseEntity.status(404).body(error);
            }
            
            redirectRepository.deleteById(id);
            
            log.info("Удален редирект ID={}", id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Редирект успешно удален");
            
            return ResponseEntity.ok().body(response);
            
        } catch (Exception e) {
            log.error("Ошибка при удалении редиректа: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Ошибка при удалении редиректа: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * API для удаления старых 404 ошибок
     */
    @PostMapping("/api/404-errors/cleanup")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> cleanupOldErrors(
            @RequestParam(defaultValue = "30") int days) {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
            notFoundErrorRepository.deleteByLastOccurredBefore(cutoffDate);
            
            log.info("Удалены 404 ошибки старше {} дней", days);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Старые ошибки удалены");
            
            return ResponseEntity.ok().body(response);
            
        } catch (Exception e) {
            log.error("Ошибка при очистке 404 ошибок: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Ошибка при очистке: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}





