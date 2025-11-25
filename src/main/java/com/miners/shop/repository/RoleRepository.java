package com.miners.shop.repository;

import com.miners.shop.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Репозиторий для работы с ролями
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    /**
     * Находит роль по имени
     */
    Optional<Role> findByName(String name);
}

