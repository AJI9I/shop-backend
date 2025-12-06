package com.miners.shop.repository;

import com.miners.shop.entity.CompanyMinerCustomField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Репозиторий для работы с дополнительными полями майнеров компании
 */
@Repository
public interface CompanyMinerCustomFieldRepository extends JpaRepository<CompanyMinerCustomField, Long> {
    
    /**
     * Получить все дополнительные поля для майнера компании, отсортированные по порядку отображения
     */
    List<CompanyMinerCustomField> findByCompanyMinerIdOrderByDisplayOrderAsc(Long companyMinerId);
}





