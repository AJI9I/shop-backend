package com.miners.shop.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Конфигурация безопасности Spring Security
 * Настраивает доступ к страницам и авторизацию
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    /**
     * Настройка цепочки фильтров безопасности
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Отключаем CSRF для упрощения (можно включить позже)
            .csrf(AbstractHttpConfigurer::disable)
            
            // Настройка доступа к страницам
            .authorizeHttpRequests(auth -> {
                auth
                    // Статические ресурсы (без авторизации)
                    .requestMatchers("/img/**", "/assets/**", "/bootstrap-theme/**").permitAll()
                    
                    // API эндпоинты (без авторизации)
                    .requestMatchers("/api/webhook/**", "/requests/api/**", "/api/products/**").permitAll()
                    
                    // Страница входа и ошибок (без авторизации)
                    .requestMatchers("/login", "/error", "/logout").permitAll()
                    
                    // Общедоступные страницы (без авторизации) - ВАЖНО: должно быть перед anyRequest()
                    .requestMatchers("/", "/products", "/products/**", "/about", "/delivery", "/services").permitAll()
                    
                    // Страница /private доступна администраторам и менеджерам
                    .requestMatchers("/private").hasAnyRole("ADMIN", "MANAGER")
                    
                    // Управление пользователями - только администраторам
                    .requestMatchers("/private/users", "/private/users/**").hasRole("ADMIN")
                    
                    // Сообщения и таблица товаров - только администраторам
                    .requestMatchers("/private/messages", "/private/messages/**").hasRole("ADMIN")
                    .requestMatchers("/private/products/table", "/private/products/table/**").hasRole("ADMIN")
                    
                    // Дашборд менеджера
                    .requestMatchers("/private/manager", "/private/manager/**").hasAnyRole("ADMIN", "MANAGER")
                    
                    // Доступ менеджера к заявкам, калькулятору и деталям майнеров
                    .requestMatchers("/private/requests", "/private/requests/**").hasAnyRole("ADMIN", "MANAGER")
                    .requestMatchers("/private/profitability", "/private/profitability/**").hasAnyRole("ADMIN", "MANAGER")
                    .requestMatchers("/private/miner-details", "/private/miner-details/**").hasAnyRole("ADMIN", "MANAGER")
                    
                    // Остальные страницы /private - только администраторам
                    .requestMatchers("/private/**").hasRole("ADMIN")
                    
                    // Все остальные страницы требуют авторизации
                    .anyRequest().authenticated();
            })
            
            // Настройка формы входа
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/private", true)
                .successHandler((request, response, authentication) -> {
                    // Перенаправляем менеджера на его дашборд, администратора - на /private
                    boolean isManager = authentication.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
                    if (isManager) {
                        response.sendRedirect(request.getContextPath() + "/private/manager");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/private");
                    }
                })
                .permitAll()
            )
            
            // Настройка выхода
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            );

        log.info("Конфигурация безопасности настроена: общедоступные страницы разрешены");
        return http.build();
    }

    /**
     * Кодировщик паролей
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

