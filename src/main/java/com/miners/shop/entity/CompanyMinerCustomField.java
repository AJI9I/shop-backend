package com.miners.shop.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Дополнительные поля для майнера компании
 */
@Entity
@Table(name = "company_miner_custom_fields")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyMinerCustomField {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_miner_id", nullable = false)
    @NotNull
    private CompanyMiner companyMiner;
    
    @Column(nullable = false, length = 255)
    @NotNull
    private String fieldName;
    
    @Column(columnDefinition = "TEXT")
    private String fieldValue;
    
    @Column(nullable = false)
    private Integer displayOrder = 0;
}









