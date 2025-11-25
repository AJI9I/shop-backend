package com.miners.shop.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Контроллер для дашборда менеджера
 */
@Controller
@RequestMapping("/private/manager")
@RequiredArgsConstructor
@Slf4j
public class ManagerController {

    /**
     * Дашборд менеджера
     */
    @GetMapping
    public String dashboard(Model model) {
        log.info("Открыт дашборд менеджера");
        model.addAttribute("pageTitle", "Дашборд менеджера");
        return "manager/dashboard";
    }
}

