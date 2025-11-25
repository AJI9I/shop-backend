package com.miners.shop.config;

import com.miners.shop.entity.Role;
import com.miners.shop.entity.User;
import com.miners.shop.repository.RoleRepository;
import com.miners.shop.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Инициализатор данных для создания ролей и администратора по умолчанию
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    @Transactional
    public void init() {
        // Создаем роли, если их нет
        createRoleIfNotExists("ADMIN");
        createRoleIfNotExists("USER");
        createRoleIfNotExists("MANAGER");

        // Создаем администратора по умолчанию, если его нет
        createAdminIfNotExists();
        
        // Создаем менеджера по умолчанию, если его нет
        createManagerIfNotExists();
    }

    /**
     * Создает роль, если она не существует
     */
    private void createRoleIfNotExists(String roleName) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            Role role = new Role();
            role.setName(roleName);
            roleRepository.save(role);
            log.info("Создана роль: {}", roleName);
        }
    }

    /**
     * Создает администратора по умолчанию, если его нет
     * Логин: admin
     * Пароль: admin
     */
    private void createAdminIfNotExists() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            // Получаем роль ADMIN
            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseThrow(() -> new RuntimeException("Роль ADMIN не найдена"));

            // Создаем нового пользователя
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin"));
            admin.setEnabled(true);

            // Создаем новый HashSet для ролей, чтобы избежать проблем с LazyInitialization
            admin.setRoles(new java.util.HashSet<>());
            admin.getRoles().add(adminRole);

            userRepository.save(admin);
            log.info("Создан администратор по умолчанию: admin/admin");
        }
    }

    /**
     * Создает менеджера по умолчанию, если его нет
     * Логин: manager
     * Пароль: manager
     */
    private void createManagerIfNotExists() {
        if (userRepository.findByUsername("manager").isEmpty()) {
            // Получаем роль MANAGER
            Role managerRole = roleRepository.findByName("MANAGER")
                    .orElseThrow(() -> new RuntimeException("Роль MANAGER не найдена"));

            // Создаем нового пользователя
            User manager = new User();
            manager.setUsername("manager");
            manager.setPassword(passwordEncoder.encode("manager"));
            manager.setEnabled(true);

            // Создаем новый HashSet для ролей
            manager.setRoles(new java.util.HashSet<>());
            manager.getRoles().add(managerRole);

            userRepository.save(manager);
            log.info("Создан менеджер по умолчанию: manager/manager");
        }
    }
}

