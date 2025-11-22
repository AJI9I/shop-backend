package com.miners.shop.repository;

import com.miners.shop.entity.Seller;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с продавцами
 */
@Repository
public interface SellerRepository extends JpaRepository<Seller, Long> {
    
    /**
     * Находит продавца по телефону
     */
    Optional<Seller> findByPhone(String phone);
    
    /**
     * Находит продавца по WhatsApp ID
     */
    Optional<Seller> findByWhatsappId(String whatsappId);
    
    /**
     * Находит продавцов с пагинацией, отсортированных по дате создания
     */
    Page<Seller> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    /**
     * Находит продавцов, отсортированных по количеству предложений (самые активные сначала)
     */
    @Query("SELECT s FROM Seller s LEFT JOIN s.offers o GROUP BY s.id ORDER BY COUNT(o.id) DESC")
    Page<Seller> findAllOrderByOffersCountDesc(Pageable pageable);
    
    /**
     * Находит продавцов, у которых есть предложения
     */
    @Query("SELECT DISTINCT s FROM Seller s JOIN s.offers o")
    List<Seller> findSellersWithOffers();
}

