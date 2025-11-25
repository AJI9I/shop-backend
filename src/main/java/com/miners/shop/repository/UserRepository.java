package com.miners.shop.repository;

import com.miners.shop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Репозиторий для работы с пользователями
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Находит пользователя по имени
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Проверяет существование пользователя с указанным именем
     */
    boolean existsByUsername(String username);
}

