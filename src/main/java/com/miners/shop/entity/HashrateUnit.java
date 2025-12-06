package com.miners.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Сущность единицы измерения хэшрейта
 */
@Entity
@Table(name = "hashrate_units")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HashrateUnit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Полное название единицы измерения
     * (например, "Терахеш в секунду")
     */
    @Column(nullable = false, length = 100)
    private String name;
    
    /**
     * Сокращение единицы измерения
     * (например, "TH/s")
     */
    @Column(nullable = false, length = 20, unique = true)
    private String abbreviation;
    
    /**
     * Множитель для конвертации в базовую единицу (H/s)
     * (например, для TH/s = 1000000000000)
     */
    @Column(nullable = false, precision = 20, scale = 0)
    private BigDecimal multiplier;
    
    /**
     * Порядок отображения в списке
     */
    @Column(nullable = false)
    private Integer displayOrder;
}








