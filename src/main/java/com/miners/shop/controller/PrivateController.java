package com.miners.shop.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Контроллер для приватной страницы администратора
 * Содержит ссылки на основные разделы: сообщения, товары, заявки
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class PrivateController {
    
    /**
     * Приватная страница с навигацией по основным разделам
     */
    @GetMapping("/private")
    public String privatePage(Model model) {
        log.info("Открыта приватная страница /private");
        
        // Можно добавить статистику для отображения на странице
        model.addAttribute("pageTitle", "Приватная панель");
        
        return "private";
    }
}








