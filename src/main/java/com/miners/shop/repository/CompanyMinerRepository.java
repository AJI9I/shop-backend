package com.miners.shop.repository;

import com.miners.shop.entity.CompanyMiner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с майнерами компании
 */
@Repository
public interface CompanyMinerRepository extends JpaRepository<CompanyMiner, Long> {
    
    /**
     * Найти майнер компании по ID MinerDetail
     */
    Optional<CompanyMiner> findByMinerDetailId(Long minerDetailId);
    
    /**
     * Проверить существование майнера компании для MinerDetail
     */
    boolean existsByMinerDetailId(Long minerDetailId);
    
    /**
     * Получить все активные майнеры компании
     */
    List<CompanyMiner> findByActiveTrue();
    
    /**
     * Найти активный майнер компании по ID MinerDetail
     */
    Optional<CompanyMiner> findByMinerDetailIdAndActiveTrue(Long minerDetailId);
    
    /**
     * Получить все майнеры компании с загрузкой всех связанных сущностей
     * Использует JOIN FETCH для избежания LazyInitializationException
     */
    @Query("SELECT DISTINCT cm FROM CompanyMiner cm " +
           "LEFT JOIN FETCH cm.minerDetail " +
           "LEFT JOIN FETCH cm.currency " +
           "LEFT JOIN FETCH cm.hashrateUnit " +
           "LEFT JOIN FETCH cm.customFields")
    List<CompanyMiner> findAllWithRelations();
}










