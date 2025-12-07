package com.miners.shop.repository;

import com.miners.shop.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с валютами
 */
@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Long> {
    
    /**
     * Найти валюту по коду
     */
    Optional<Currency> findByCode(String code);
    
    /**
     * Получить все валюты, отсортированные по порядку отображения
     */
    List<Currency> findAllByOrderByDisplayOrderAsc();
    
    /**
     * Найти базовую валюту
     */
    Optional<Currency> findByIsBaseTrue();
}















