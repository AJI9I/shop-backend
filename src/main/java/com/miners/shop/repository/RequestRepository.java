package com.miners.shop.repository;

import com.miners.shop.entity.Request;
import com.miners.shop.entity.Request.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Репозиторий для работы с заявками
 */
@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
    
    /**
     * Найти все заявки с пагинацией
     */
    Page<Request> findAll(Pageable pageable);
    
    /**
     * Найти заявки по статусу с пагинацией
     */
    Page<Request> findByStatus(RequestStatus status, Pageable pageable);
    
    /**
     * Найти заявки по предложению
     */
    List<Request> findByOfferId(Long offerId);
    
    /**
     * Найти заявки по телефону клиента
     */
    List<Request> findByClientPhone(String clientPhone);
    
    /**
     * Подсчитать количество заявок по статусу
     */
    long countByStatus(RequestStatus status);
    
    /**
     * Найти заявки с информацией о предложении и сообщении
     */
    @Query("SELECT r FROM Request r " +
           "LEFT JOIN FETCH r.offer o " +
           "LEFT JOIN FETCH o.product p " +
           "LEFT JOIN FETCH o.seller s " +
           "LEFT JOIN FETCH r.whatsAppMessage w " +
           "ORDER BY r.createdAt DESC")
    Page<Request> findAllWithDetails(Pageable pageable);
    
    /**
     * Найти заявку по ID с полной информацией
     */
    @Query("SELECT r FROM Request r " +
           "LEFT JOIN FETCH r.offer o " +
           "LEFT JOIN FETCH o.product p " +
           "LEFT JOIN FETCH o.seller s " +
           "LEFT JOIN FETCH r.whatsAppMessage w " +
           "WHERE r.id = :id")
    Request findByIdWithDetails(@Param("id") Long id);
}


