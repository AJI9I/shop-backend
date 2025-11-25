package com.miners.shop.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Контроллер для информационных страниц: О нас, Доставка, Услуги
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class InfoController {
    
    /**
     * Страница "О нас"
     */
    @GetMapping("/about")
    public String about(Model model) {
        log.info("Открыта страница О нас");
        model.addAttribute("pageTitle", "О нас - MinerHive");
        return "about";
    }
    
    /**
     * Страница "Доставка"
     */
    @GetMapping("/delivery")
    public String delivery(Model model) {
        log.info("Открыта страница Доставка");
        model.addAttribute("pageTitle", "Доставка - MinerHive");
        return "delivery";
    }
    
    /**
     * Страница "Услуги"
     */
    @GetMapping("/services")
    public String services(Model model) {
        log.info("Открыта страница Услуги");
        model.addAttribute("pageTitle", "Услуги - MinerHive");
        return "services";
    }
}








