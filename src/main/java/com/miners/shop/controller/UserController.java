package com.miners.shop.controller;

import com.miners.shop.dto.UserDTO;
import com.miners.shop.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Контроллер для управления пользователями
 */
@Controller
@RequestMapping("/private/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * Список всех пользователей
     */
    @GetMapping
    public String listUsers(Model model) {
        List<UserDTO> users = userService.getAllUsers();
        model.addAttribute("users", users);
        model.addAttribute("pageTitle", "Управление пользователями");
        return "users/list";
    }

    /**
     * Форма создания нового пользователя
     */
    @GetMapping("/new")
    public String createUserForm(Model model) {
        UserDTO userDTO = new UserDTO();
        userDTO.setEnabled(true);
        userDTO.setRoles(new java.util.HashSet<>());
        model.addAttribute("user", userDTO);
        model.addAttribute("allRoles", userService.getAllRoles());
        model.addAttribute("pageTitle", "Создать пользователя");
        return "users/create";
    }

    /**
     * Создание нового пользователя
     */
    @PostMapping("/new")
    public String createUser(@Valid @ModelAttribute("user") UserDTO userDTO,
                             @RequestParam(value = "roles", required = false) String[] rolesArray,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        // Обрабатываем роли из формы
        if (rolesArray != null && rolesArray.length > 0) {
            userDTO.setRoles(new java.util.HashSet<>(java.util.Arrays.asList(rolesArray)));
        } else {
            userDTO.setRoles(new java.util.HashSet<>());
        }
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("allRoles", userService.getAllRoles());
            return "users/create";
        }

        try {
            userService.createUser(userDTO);
            redirectAttributes.addFlashAttribute("success", "Пользователь успешно создан");
            return "redirect:/private/users";
        } catch (Exception e) {
            log.error("Ошибка при создании пользователя", e);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("allRoles", userService.getAllRoles());
            return "users/create";
        }
    }

    /**
     * Форма редактирования пользователя
     */
    @GetMapping("/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        UserDTO userDTO = userService.getUserById(id);
        model.addAttribute("user", userDTO);
        model.addAttribute("allRoles", userService.getAllRoles());
        model.addAttribute("pageTitle", "Редактировать пользователя");
        return "users/edit";
    }

    /**
     * Обновление пользователя
     */
    @PostMapping("/{id}/edit")
    public String updateUser(@PathVariable Long id,
                            @Valid @ModelAttribute("user") UserDTO userDTO,
                            @RequestParam(value = "roles", required = false) String[] rolesArray,
                            BindingResult bindingResult,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        // Обрабатываем роли из формы
        if (rolesArray != null && rolesArray.length > 0) {
            userDTO.setRoles(new java.util.HashSet<>(java.util.Arrays.asList(rolesArray)));
        } else {
            userDTO.setRoles(new java.util.HashSet<>());
        }
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("allRoles", userService.getAllRoles());
            return "users/edit";
        }

        try {
            userService.updateUser(id, userDTO);
            redirectAttributes.addFlashAttribute("success", "Пользователь успешно обновлен");
            return "redirect:/private/users";
        } catch (Exception e) {
            log.error("Ошибка при обновлении пользователя", e);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("allRoles", userService.getAllRoles());
            return "users/edit";
        }
    }

    /**
     * Удаление пользователя
     */
    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "Пользователь успешно удален");
        } catch (Exception e) {
            log.error("Ошибка при удалении пользователя", e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении пользователя: " + e.getMessage());
        }
        return "redirect:/private/users";
    }
}

