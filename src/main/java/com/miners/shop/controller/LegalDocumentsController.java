package com.miners.shop.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Контроллер для отображения юридических документов
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class LegalDocumentsController {
    
    /**
     * Политика использования cookies
     */
    @GetMapping("/cookies-policy")
    public String cookiesPolicy(Model model) {
        model.addAttribute("title", "Политика использования cookies");
        return "legal/cookies-policy";
    }
    
    /**
     * Политика конфиденциальности
     */
    @GetMapping("/privacy-policy")
    public String privacyPolicy(Model model) {
        model.addAttribute("title", "Политика конфиденциальности");
        return "legal/privacy-policy";
    }
    
    /**
     * Согласие на обработку персональных данных
     */
    @GetMapping("/personal-data-consent")
    public String personalDataConsent(Model model) {
        model.addAttribute("title", "Согласие на обработку персональных данных");
        return "legal/personal-data-consent";
    }
    
    /**
     * Публичная оферта (договор)
     */
    @GetMapping("/offer")
    public String offer(Model model) {
        model.addAttribute("title", "Публичная оферта");
        return "legal/offer";
    }
    
    /**
     * Пользовательское соглашение
     */
    @GetMapping("/user-agreement")
    public String userAgreement(Model model) {
        model.addAttribute("title", "Пользовательское соглашение");
        return "legal/user-agreement";
    }
    
    /**
     * Реквизиты компании
     */
    @GetMapping("/company-details")
    public String companyDetails(Model model) {
        model.addAttribute("title", "Реквизиты компании");
        return "legal/company-details";
    }
}



