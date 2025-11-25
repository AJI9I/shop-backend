package com.miners.shop.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Контроллер для страницы входа
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class LoginController {

    /**
     * Страница входа
     */
    @GetMapping("/login")
    public String login(Model model) {
        log.info("Открыта страница входа");
        return "login";
    }
}

