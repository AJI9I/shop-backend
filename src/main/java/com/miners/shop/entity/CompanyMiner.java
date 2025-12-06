package com.miners.shop.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Сущность майнера компании
 * Связана один-к-одному с MinerDetail
 */
@Entity
@Table(name = "company_miners")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyMiner {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "miner_detail_id", nullable = false, unique = true)
    @NotNull
    private MinerDetail minerDetail;
    
    @Column(nullable = false, precision = 20, scale = 2)
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id", nullable = false)
    @NotNull
    private Currency currency;
    
    @Transient
    private BigDecimal priceConverted;
    
    @Column(precision = 20, scale = 2)
    private BigDecimal priceOld;
    
    @Column(nullable = false, precision = 20, scale = 2)
    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal hashrateMin;
    
    @Column(precision = 20, scale = 2)
    private BigDecimal hashrateMax;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hashrate_unit_id", nullable = false)
    @NotNull
    private HashrateUnit hashrateUnit;
    
    @Column(nullable = false)
    @NotNull
    @Min(0)
    private Integer quantity;
    
    @Column(length = 500)
    private String condition;
    
    @Column(nullable = false)
    @NotNull
    private Boolean active = true;
    
    @Column
    private Integer lowStockThreshold;
    
    @OneToMany(mappedBy = "companyMiner", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CompanyMinerCustomField> customFields = new ArrayList<>();
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}


