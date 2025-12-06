package com.miners.shop.controller;

import com.miners.shop.util.SeoUtil;
import jakarta.servlet.http.HttpServletRequest;
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
    public String about(HttpServletRequest request, Model model) {
        log.info("Открыта страница О нас");
        model.addAttribute("pageTitle", "О нас - MinerHive");
        model.addAttribute("pageDescription", "MinerHive - агрегатор предложений по майнинг-оборудованию. Помогаем найти выгодные предложения на рынке ASIC майнеров.");
        model.addAttribute("pageKeywords", "MinerHive, о нас, майнинг, ASIC майнеры");
        model.addAttribute("canonicalUrl", SeoUtil.generateCanonicalUrl(request));
        model.addAttribute("ogImage", "https://minerhive.ru/assets/images/logo/logo.png");
        return "about";
    }
    
    /**
     * Страница "Доставка"
     */
    @GetMapping("/delivery")
    public String delivery(HttpServletRequest request, Model model) {
        log.info("Открыта страница Доставка");
        model.addAttribute("pageTitle", "Доставка - MinerHive");
        model.addAttribute("pageDescription", "Информация о доставке майнинг-оборудования. Условия доставки, сроки, стоимость.");
        model.addAttribute("pageKeywords", "доставка майнеров, доставка ASIC, условия доставки");
        model.addAttribute("canonicalUrl", SeoUtil.generateCanonicalUrl(request));
        model.addAttribute("ogImage", "https://minerhive.ru/assets/images/logo/logo.png");
        return "delivery";
    }
    
    /**
     * Страница "Услуги"
     */
    @GetMapping("/services")
    public String services(HttpServletRequest request, Model model) {
        log.info("Открыта страница Услуги");
        model.addAttribute("pageTitle", "Услуги - MinerHive");
        model.addAttribute("pageDescription", "Услуги MinerHive: помощь в выборе майнинг-оборудования, консультации, подбор оптимальных решений для майнинга.");
        model.addAttribute("pageKeywords", "услуги майнинг, консультации по майнингу, подбор майнеров");
        model.addAttribute("canonicalUrl", SeoUtil.generateCanonicalUrl(request));
        model.addAttribute("ogImage", "https://minerhive.ru/assets/images/logo/logo.png");
        return "services";
    }
}








