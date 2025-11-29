package com.miners.shop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Контроллер для редиректов старых путей на новые
 * Обеспечивает обратную совместимость со старыми ссылками
 */
@Controller
public class RedirectController {
    
    /**
     * Редирект со старого пути /messages/{id} на новый /private/messages/{id}
     * Для обратной совместимости со старыми ссылками
     */
    @GetMapping("/messages/{id}")
    public String redirectToPrivateMessage(@PathVariable Long id) {
        return "redirect:/private/messages/" + id;
    }
    
    /**
     * Редирект со старого пути /messages на новый /private/messages
     * Для обратной совместимости со старыми ссылками
     */
    @GetMapping("/messages")
    public String redirectToPrivateMessages() {
        return "redirect:/private/messages";
    }
}

