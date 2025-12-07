package com.miners.shop.repository;

import com.miners.shop.entity.HashrateUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с единицами измерения хэшрейта
 */
@Repository
public interface HashrateUnitRepository extends JpaRepository<HashrateUnit, Long> {
    
    /**
     * Найти единицу измерения по сокращению
     */
    Optional<HashrateUnit> findByAbbreviation(String abbreviation);
    
    /**
     * Получить все единицы измерения, отсортированные по порядку отображения
     */
    List<HashrateUnit> findAllByOrderByDisplayOrderAsc();
}















