package com.miners.shop.repository;

import com.miners.shop.entity.Offer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {
    
    /**
     * Находит все предложения для товара с загрузкой продавцов
     * Использует JOIN FETCH для избежания LazyInitializationException
     */
    @Query("SELECT o FROM Offer o LEFT JOIN FETCH o.seller WHERE o.product.id = :productId ORDER BY o.price ASC")
    List<Offer> findByProductIdOrderByPriceAsc(@Param("productId") Long productId);
    
    /**
     * Находит все предложения с пагинацией, отсортированные по дате создания
     */
    Page<Offer> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    
    /**
     * Находит предложения по товару с пагинацией
     */
    Page<Offer> findByProductIdOrderByPriceAsc(Long productId, Pageable pageable);
    
    /**
     * Находит предложения по товару с фильтрацией по дате и пагинацией
     * Сортировка применяется из Pageable (не жестко задана в запросе)
     */
    @Query("SELECT o FROM Offer o WHERE o.product.id = :productId AND o.updatedAt >= :dateFrom")
    Page<Offer> findByProductIdAndUpdatedAtGreaterThanEqual(@Param("productId") Long productId, @Param("dateFrom") LocalDateTime dateFrom, Pageable pageable);
    
    /**
     * Находит предложения по товару с пагинацией (без фильтра по дате)
     */
    @Query("SELECT o FROM Offer o WHERE o.product.id = :productId")
    Page<Offer> findByProductIdWithSeller(@Param("productId") Long productId, Pageable pageable);
    
    /**
     * Находит предложения по товару с фильтрацией по дате, типу операции и наличию цены
     * Не использует JOIN FETCH в основном запросе, чтобы избежать проблем с пагинацией
     * Seller загружается отдельно в сервисе через батч-загрузку
     * 
     * ВАЖНО: Используется нативный SQL для PostgreSQL с правильной обработкой NULL параметров.
     * Для PostgreSQL используем COALESCE с минимальной датой для явного указания типа параметра.
     * Это позволяет PostgreSQL правильно определить тип параметра даже когда он NULL.
     * 
     * ВАЖНО: ORDER BY использует явное имя колонки в snake_case (updated_at), чтобы избежать
     * ошибки "столбец updatedat не существует". LIMIT и OFFSET добавляются через SpEL выражения.
     * 
     * @param productId ID товара
     * @param dateFrom Дата начала периода (может быть null)
     * @param operationType Тип операции: SELL или BUY (может быть null)
     * @param hasPrice Только с ценой (true) или все (false, если null)
     * @param pageable Пагинация и сортировка
     */
    /**
     * Подсчитывает общее количество предложений с фильтрами (для пагинации)
     */
    @Query(value = "SELECT COUNT(*) FROM offers WHERE product_id = :productId " +
           "AND updated_at >= COALESCE(:dateFrom, '1900-01-01'::timestamp) " +
           "AND (CAST(:operationType AS varchar) IS NULL OR operation_type = CAST(:operationType AS varchar))",
           nativeQuery = true)
    long countByProductIdWithFilters(
            @Param("productId") Long productId,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("operationType") String operationType);
    
    /**
     * Находит предложения от продавца для конкретной модели товара
     * Используется для обнаружения дубликатов при обновлении предложений
     */
    List<Offer> findByProductIdAndSellerId(Long productId, Long sellerId);
    
    /**
     * Находит предложения от продавца по ID
     * Используется для поиска всех предложений продавца при обновлении
     */
    List<Offer> findBySellerId(Long sellerId);
    
    /**
     * Находит предложения по ID сообщения источника
     */
    List<Offer> findBySourceMessageId(String sourceMessageId);
    
    /**
     * Находит все предложения для списка товаров с загрузкой продавцов
     * Используется для батч-загрузки offers для нескольких продуктов
     */
    @Query("SELECT o FROM Offer o LEFT JOIN FETCH o.seller WHERE o.product.id IN :productIds")
    List<Offer> findByProductIdIn(@Param("productIds") List<Long> productIds);
    
    /**
     * Находит предложения от продавца для конкретной модели товара (по телефону)
     * @deprecated Используйте findByProductIdAndSellerId
     */
    @Deprecated
    List<Offer> findByProductIdAndSellerPhone(Long productId, String sellerPhone);
    
    /**
     * Находит предложения от продавца по телефону
     * @deprecated Используйте findBySellerId
     */
    @Deprecated
    List<Offer> findBySellerPhone(String sellerPhone);
    
    /**
     * Находит минимальные цены для списка товаров по типу операции
     * Возвращает массив [productId, minPrice] для каждого товара
     */
    @Query("SELECT o.product.id, MIN(o.price) FROM Offer o " +
           "WHERE o.product.id IN :productIds " +
           "AND o.operationType = :operationType " +
           "AND o.price IS NOT NULL " +
           "GROUP BY o.product.id")
    List<Object[]> findMinPriceByProductIdsAndOperationType(
            @Param("productIds") List<Long> productIds,
            @Param("operationType") com.miners.shop.entity.OperationType operationType);
    
    /**
     * Находит все предложения с фильтрацией по производителю и типу операции
     * Сортировка по дате создания (от последнего)
     */
    @Query("SELECT o FROM Offer o " +
           "WHERE (:manufacturer IS NULL OR o.manufacturer = :manufacturer) " +
           "AND (:operationType IS NULL OR o.operationType = :operationType) " +
           "ORDER BY o.createdAt DESC")
    Page<Offer> findByManufacturerAndOperationTypeOrderByCreatedAtDesc(
            @Param("manufacturer") String manufacturer,
            @Param("operationType") com.miners.shop.entity.OperationType operationType,
            Pageable pageable);
    
    /**
     * Получает список уникальных производителей из предложений
     * Используется для заполнения фильтра по производителю
     */
    @Query("SELECT DISTINCT o.manufacturer FROM Offer o WHERE o.manufacturer IS NOT NULL AND o.manufacturer != '' ORDER BY o.manufacturer")
    List<String> findDistinctManufacturers();
    
    /**
     * Находит предложения с фильтрацией по производителю, типу операции и дате создания
     * Сортировка по дате создания (от последнего)
     */
    @Query("SELECT o FROM Offer o " +
           "WHERE (:manufacturer IS NULL OR o.manufacturer = :manufacturer) " +
           "AND (:operationType IS NULL OR o.operationType = :operationType) " +
           "AND (:dateFrom IS NULL OR o.createdAt >= :dateFrom) " +
           "ORDER BY o.createdAt DESC")
    Page<Offer> findByManufacturerAndOperationTypeAndDateOrderByCreatedAtDesc(
            @Param("manufacturer") String manufacturer,
            @Param("operationType") com.miners.shop.entity.OperationType operationType,
            @Param("dateFrom") LocalDateTime dateFrom,
            Pageable pageable);
}

