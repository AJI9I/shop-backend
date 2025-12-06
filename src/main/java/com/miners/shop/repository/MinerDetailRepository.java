package com.miners.shop.repository;

import com.miners.shop.entity.MinerDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с детальной информацией о майнерах
 */
@Repository
public interface MinerDetailRepository extends JpaRepository<MinerDetail, Long> {
    
    /**
     * Найти детальную запись по стандартизированному названию
     */
    Optional<MinerDetail> findByStandardName(String standardName);
    
    /**
     * Найти детальные записи по производителю
     */
    List<MinerDetail> findByManufacturer(String manufacturer);
    
    /**
     * Найти детальные записи по серии
     */
    List<MinerDetail> findBySeries(String series);
    
    /**
     * Найти детальные записи по алгоритму
     */
    List<MinerDetail> findByAlgorithm(String algorithm);
    
    /**
     * Получить список уникальных производителей
     * Возвращает только производителей активных MinerDetail (active = true)
     */
    @Query("SELECT DISTINCT md.manufacturer FROM MinerDetail md WHERE md.manufacturer IS NOT NULL AND md.manufacturer != '' AND (md.active IS NULL OR md.active = true) ORDER BY md.manufacturer")
    List<String> findDistinctManufacturers();
    
    /**
     * Получить список уникальных серий
     * Возвращает только серии активных MinerDetail (active = true)
     */
    @Query("SELECT DISTINCT md.series FROM MinerDetail md WHERE md.series IS NOT NULL AND md.series != '' AND (md.active IS NULL OR md.active = true) ORDER BY md.series")
    List<String> findDistinctSeries();
    
    /**
     * Получить список уникальных серий для выбранных производителей
     * Возвращает только серии активных MinerDetail (active = true)
     */
    @Query("SELECT DISTINCT md.series FROM MinerDetail md WHERE md.series IS NOT NULL AND md.series != '' AND md.manufacturer IN :manufacturers AND (md.active IS NULL OR md.active = true) ORDER BY md.series")
    List<String> findDistinctSeriesByManufacturers(
            @org.springframework.data.repository.query.Param("manufacturers") java.util.List<String> manufacturers);
    
    /**
     * Получить маппинг серия -> производитель для всех MinerDetail
     * Возвращает список объектов, каждый содержит series и manufacturer
     * Возвращает только маппинги для активных MinerDetail (active = true)
     */
    @Query("SELECT DISTINCT md.series as series, md.manufacturer as manufacturer FROM MinerDetail md WHERE md.series IS NOT NULL AND md.series != '' AND md.manufacturer IS NOT NULL AND md.manufacturer != '' AND (md.active IS NULL OR md.active = true)")
    List<Object[]> findSeriesManufacturerMapping();
    
    /**
     * Получить список уникальных алгоритмов
     */
    @Query("SELECT DISTINCT md.algorithm FROM MinerDetail md WHERE md.algorithm IS NOT NULL AND md.algorithm != '' ORDER BY md.algorithm")
    List<String> findDistinctAlgorithms();
    
    /**
     * Находит все MinerDetail, у которых есть предложения
     * Сортировка будет применена в сервисе для поддержки сортировки по MAX(offer.updated_at)
     * Показываются только активные MinerDetail (active = true)
     */
    @Query("SELECT DISTINCT md FROM MinerDetail md " +
           "INNER JOIN md.products p " +
           "INNER JOIN p.offers o " +
           "WHERE o.id IS NOT NULL AND (md.active IS NULL OR md.active = true)")
    List<MinerDetail> findAllWithOffers();
    
    /**
     * Подсчитывает количество MinerDetail, у которых есть предложения
     * Считаются только активные MinerDetail (active = true)
     */
    @Query("SELECT COUNT(DISTINCT md) FROM MinerDetail md " +
           "INNER JOIN md.products p " +
           "INNER JOIN p.offers o " +
           "WHERE o.id IS NOT NULL AND (md.active IS NULL OR md.active = true)")
    long countAllWithOffers();
    
    /**
     * Находит все MinerDetail, отсортированные по алфавиту (standardName)
     */
    Page<MinerDetail> findAllByOrderByStandardNameAsc(Pageable pageable);
    
    /**
     * Находит все MinerDetail, отсортированные по производителю, затем по названию
     */
    @Query("SELECT md FROM MinerDetail md ORDER BY md.manufacturer ASC, md.standardName ASC")
    Page<MinerDetail> findAllByOrderByManufacturerAscStandardNameAsc(Pageable pageable);
    
    /**
     * Находит все MinerDetail, у которых есть предложения, с фильтрацией по производителю
     * Показываются только активные MinerDetail (active = true)
     */
    @Query("SELECT DISTINCT md FROM MinerDetail md " +
           "INNER JOIN md.products p " +
           "INNER JOIN p.offers o " +
           "WHERE o.id IS NOT NULL AND md.manufacturer IN :manufacturers AND (md.active IS NULL OR md.active = true)")
    List<MinerDetail> findAllWithOffersByManufacturers(
            @org.springframework.data.repository.query.Param("manufacturers") java.util.List<String> manufacturers);
    
    /**
     * Находит все MinerDetail, у которых есть предложения, с фильтрацией по сериям
     * Показываются только активные MinerDetail (active = true)
     */
    @Query("SELECT DISTINCT md FROM MinerDetail md " +
           "INNER JOIN md.products p " +
           "INNER JOIN p.offers o " +
           "WHERE o.id IS NOT NULL AND md.series IN :series AND (md.active IS NULL OR md.active = true)")
    List<MinerDetail> findAllWithOffersBySeries(
            @org.springframework.data.repository.query.Param("series") java.util.List<String> series);
    
    /**
     * Находит все MinerDetail, у которых есть предложения, с фильтрацией по производителю и сериям
     * Показываются только активные MinerDetail (active = true)
     */
    @Query("SELECT DISTINCT md FROM MinerDetail md " +
           "INNER JOIN md.products p " +
           "INNER JOIN p.offers o " +
           "WHERE o.id IS NOT NULL AND md.manufacturer IN :manufacturers AND md.series IN :series AND (md.active IS NULL OR md.active = true)")
    List<MinerDetail> findAllWithOffersByManufacturersAndSeries(
            @org.springframework.data.repository.query.Param("manufacturers") java.util.List<String> manufacturers,
            @org.springframework.data.repository.query.Param("series") java.util.List<String> series);
    
    /**
     * Находит все MinerDetail, у которых есть предложения, с поиском по названию, производителю или серии
     * Поиск выполняется по стандартному названию, производителю и серии (без учета регистра)
     * Показываются только активные MinerDetail (active = true)
     */
    @Query("SELECT DISTINCT md FROM MinerDetail md " +
           "INNER JOIN md.products p " +
           "INNER JOIN p.offers o " +
           "WHERE o.id IS NOT NULL AND (md.active IS NULL OR md.active = true) AND " +
           "(LOWER(md.standardName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(md.manufacturer) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(md.series) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<MinerDetail> findAllWithOffersBySearch(@Param("search") String search);
    
    /**
     * Находит все MinerDetail с пагинацией, поиском и сортировкой
     * Поддерживает поиск по standardName, manufacturer и series
     */
    @Query("SELECT md FROM MinerDetail md WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(md.standardName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(md.manufacturer) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(md.series) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY md.standardName ASC")
    Page<MinerDetail> findAllBySearchOrderByStandardNameAsc(
            @Param("search") String search,
            Pageable pageable);
    
    /**
     * Проверяет существование MinerDetail с указанным slug
     */
    boolean existsBySlug(String slug);
    
    /**
     * Находит MinerDetail по slug
     */
    Optional<MinerDetail> findBySlug(String slug);
}

