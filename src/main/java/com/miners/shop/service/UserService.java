package com.miners.shop.service;

import com.miners.shop.dto.UserDTO;
import com.miners.shop.entity.Role;
import com.miners.shop.entity.User;
import com.miners.shop.repository.RoleRepository;
import com.miners.shop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Сервис для управления пользователями
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Получить всех пользователей
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Получить пользователя по ID
     */
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + id));
        return convertToDTO(user);
    }

    /**
     * Создать нового пользователя
     */
    @Transactional
    public UserDTO createUser(UserDTO userDTO) {
        if (userRepository.findByUsername(userDTO.getUsername()).isPresent()) {
            throw new RuntimeException("Пользователь с таким именем уже существует");
        }

        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setEnabled(userDTO.getEnabled() != null ? userDTO.getEnabled() : true);

        // Устанавливаем роли
        Set<Role> roles = new HashSet<>();
        for (String roleName : userDTO.getRoles()) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Роль не найдена: " + roleName));
            roles.add(role);
        }
        user.setRoles(roles);

        User saved = userRepository.save(user);
        log.info("Создан пользователь: {}", saved.getUsername());
        return convertToDTO(saved);
    }

    /**
     * Обновить пользователя
     */
    @Transactional
    public UserDTO updateUser(Long id, UserDTO userDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + id));

        // Проверяем уникальность имени пользователя (если изменилось)
        if (!user.getUsername().equals(userDTO.getUsername())) {
            if (userRepository.findByUsername(userDTO.getUsername()).isPresent()) {
                throw new RuntimeException("Пользователь с таким именем уже существует");
            }
            user.setUsername(userDTO.getUsername());
        }

        // Обновляем пароль только если он указан
        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        user.setEnabled(userDTO.getEnabled() != null ? userDTO.getEnabled() : true);

        // Обновляем роли
        Set<Role> roles = new HashSet<>();
        for (String roleName : userDTO.getRoles()) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Роль не найдена: " + roleName));
            roles.add(role);
        }
        user.setRoles(roles);

        User saved = userRepository.save(user);
        log.info("Обновлен пользователь: {}", saved.getUsername());
        return convertToDTO(saved);
    }

    /**
     * Удалить пользователя
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + id));
        userRepository.delete(user);
        log.info("Удален пользователь: {}", user.getUsername());
    }

    /**
     * Получить все доступные роли
     */
    @Transactional(readOnly = true)
    public List<String> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(Role::getName)
                .collect(Collectors.toList());
    }

    /**
     * Преобразовать User в UserDTO
     */
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEnabled(user.getEnabled());
        dto.setRoles(user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet()));
        return dto;
    }
}

