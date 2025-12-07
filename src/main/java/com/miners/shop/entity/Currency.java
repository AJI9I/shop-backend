package com.miners.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Сущность валюты
 */
@Entity
@Table(name = "currencies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Currency {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Код валюты (RUB, USD, EUR, CNY)
     */
    @Column(nullable = false, length = 10, unique = true)
    private String code;
    
    /**
     * Название валюты
     * (например, "Российский рубль", "Доллар США")
     */
    @Column(nullable = false, length = 100)
    private String name;
    
    /**
     * Символ валюты
     * (например, "₽", "$", "€", "¥")
     */
    @Column(nullable = false, length = 10)
    private String symbol;
    
    /**
     * Базовая валюта (для RUB = true)
     */
    @Column(nullable = false)
    private Boolean isBase = false;
    
    /**
     * Порядок отображения в списке
     */
    @Column(nullable = false)
    private Integer displayOrder;
}









