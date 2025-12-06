package com.miners.shop.repository;

import com.miners.shop.entity.MinerDetail;
import com.miners.shop.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    /**
     * Находит товар по модели
     */
    Optional<Product> findByModel(String model);
    
    /**
     * Находит все товары с пагинацией, отсортированные по дате обновления
     */
    Page<Product> findAllByOrderByUpdatedAtDesc(Pageable pageable);
    
    /**
     * Находит товары, у которых есть предложения
     * Загружает предложения и продавцов для отображения
     * Использует DISTINCT для избежания дубликатов при JOIN FETCH
     */
    @Query(value = "SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.offers o " +
           "LEFT JOIN FETCH o.seller " +
           "WHERE EXISTS (SELECT 1 FROM Offer o2 WHERE o2.product.id = p.id) " +
           "ORDER BY p.updatedAt DESC",
           countQuery = "SELECT COUNT(DISTINCT p) FROM Product p WHERE EXISTS (SELECT 1 FROM Offer o2 WHERE o2.product.id = p.id)")
    Page<Product> findProductsWithOffers(Pageable pageable);
    
    /**
     * Находит товары по производителю с пагинацией
     */
    Page<Product> findByManufacturer(String manufacturer, Pageable pageable);
    
    /**
     * Получает список уникальных производителей
     */
    @Query("SELECT DISTINCT p.manufacturer FROM Product p WHERE p.manufacturer IS NOT NULL AND p.manufacturer != '' ORDER BY p.manufacturer")
    List<String> findDistinctManufacturers();
    
    /**
     * Находит все товары, связанные с MinerDetail
     */
    List<Product> findByMinerDetail(MinerDetail minerDetail);
    
    /**
     * Находит все товары, связанные с MinerDetail по ID
     */
    @Query("SELECT p FROM Product p WHERE p.minerDetail.id = :minerDetailId")
    List<Product> findByMinerDetailId(@Param("minerDetailId") Long minerDetailId);
    
    /**
     * Находит все товары, связанные с несколькими MinerDetail по их ID (оптимизация для избежания N+1)
     */
    @Query("SELECT p FROM Product p WHERE p.minerDetail.id IN :minerDetailIds")
    List<Product> findByMinerDetailIdIn(@Param("minerDetailIds") List<Long> minerDetailIds);
}

