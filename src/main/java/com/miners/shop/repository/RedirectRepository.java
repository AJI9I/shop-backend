package com.miners.shop.repository;

import com.miners.shop.entity.Redirect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Репозиторий для работы с редиректами
 */
@Repository
public interface RedirectRepository extends JpaRepository<Redirect, Long> {
    
    /**
     * Найти редирект по старому URL
     */
    Optional<Redirect> findByFromUrl(String fromUrl);
    
    /**
     * Найти активный редирект по старому URL
     */
    Optional<Redirect> findByFromUrlAndActiveTrue(String fromUrl);
    
    /**
     * Проверить существование редиректа для URL
     */
    boolean existsByFromUrl(String fromUrl);
}





