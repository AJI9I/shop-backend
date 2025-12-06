package com.miners.shop.repository;

import com.miners.shop.entity.NotFoundError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с 404 ошибками
 */
@Repository
public interface NotFoundErrorRepository extends JpaRepository<NotFoundError, Long> {
    
    /**
     * Найти ошибку по URL
     */
    Optional<NotFoundError> findByUrl(String url);
    
    /**
     * Получить все ошибки с пагинацией, отсортированные по дате последнего запроса
     */
    Page<NotFoundError> findAllByOrderByLastOccurredDesc(Pageable pageable);
    
    /**
     * Получить топ ошибок по количеству запросов
     */
    @Query("SELECT nfe FROM NotFoundError nfe ORDER BY nfe.count DESC, nfe.lastOccurred DESC")
    List<NotFoundError> findTopErrors(Pageable pageable);
    
    /**
     * Получить ошибки за последние N дней
     */
    @Query("SELECT nfe FROM NotFoundError nfe WHERE nfe.lastOccurred >= :since ORDER BY nfe.lastOccurred DESC")
    List<NotFoundError> findRecentErrors(LocalDateTime since);
    
    /**
     * Удалить старые ошибки (старше указанной даты)
     */
    void deleteByLastOccurredBefore(LocalDateTime date);
}





