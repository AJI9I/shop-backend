package com.miners.shop.dto;

import com.miners.shop.entity.CompanyMiner;
import com.miners.shop.entity.Currency;
import com.miners.shop.entity.HashrateUnit;
import com.miners.shop.entity.MinerDetail;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO для майнера компании
 */
public class CompanyMinerDTO {
    
    /**
     * Полный DTO для отображения
     */
    @Builder
    public record CompanyMinerInfo(
            Long id,
            Long minerDetailId,
            String minerDetailName,
            BigDecimal price,
            Long currencyId,
            String currencyCode,
            String currencySymbol,
            BigDecimal priceConverted,
            BigDecimal priceOld,
            BigDecimal hashrateMin,
            BigDecimal hashrateMax,
            Long hashrateUnitId,
            String hashrateUnitAbbreviation,
            Integer quantity,
            String condition,
            Boolean active,
            Integer lowStockThreshold,
            List<CustomFieldInfo> customFields,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
    
    /**
     * Информация о дополнительном поле
     */
    @Builder
    public record CustomFieldInfo(
            Long id,
            String fieldName,
            String fieldValue,
            Integer displayOrder
    ) {}
    
    /**
     * DTO для создания майнера компании
     */
    @Builder
    public record CreateCompanyMinerDTO(
            @NotNull Long minerDetailId,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
            @NotNull Long currencyId,
            BigDecimal priceOld,
            @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal hashrateMin,
            BigDecimal hashrateMax,
            @NotNull Long hashrateUnitId,
            @NotNull @Min(0) Integer quantity,
            String condition,
            Boolean active,
            Integer lowStockThreshold,
            List<CreateCustomFieldDTO> customFields
    ) {}
    
    /**
     * DTO для создания дополнительного поля
     */
    @Builder
    public record CreateCustomFieldDTO(
            String fieldName,
            String fieldValue,
            Integer displayOrder
    ) {}
    
    /**
     * DTO для обновления майнера компании
     */
    @Builder
    public record UpdateCompanyMinerDTO(
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
            @NotNull Long currencyId,
            BigDecimal priceOld,
            @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal hashrateMin,
            BigDecimal hashrateMax,
            @NotNull Long hashrateUnitId,
            @NotNull @Min(0) Integer quantity,
            String condition,
            Boolean active,
            Integer lowStockThreshold,
            List<UpdateCustomFieldDTO> customFields
    ) {}
    
    /**
     * DTO для обновления дополнительного поля
     */
    @Builder
    public record UpdateCustomFieldDTO(
            String fieldName,
            String fieldValue,
            Integer displayOrder
    ) {}
    
    /**
     * Преобразует сущность в DTO
     */
    public static CompanyMinerInfo fromEntity(CompanyMiner companyMiner) {
        if (companyMiner == null) {
            return null;
        }
        
        Currency currency = companyMiner.getCurrency();
        HashrateUnit hashrateUnit = companyMiner.getHashrateUnit();
        MinerDetail minerDetail = companyMiner.getMinerDetail();
        
        List<CustomFieldInfo> customFields = companyMiner.getCustomFields() != null
                ? companyMiner.getCustomFields().stream()
                        .map(field -> CustomFieldInfo.builder()
                                .id(field.getId())
                                .fieldName(field.getFieldName())
                                .fieldValue(field.getFieldValue())
                                .displayOrder(field.getDisplayOrder())
                                .build())
                        .collect(Collectors.toList())
                : List.of();
        
        return CompanyMinerInfo.builder()
                .id(companyMiner.getId())
                .minerDetailId(minerDetail != null ? minerDetail.getId() : null)
                .minerDetailName(minerDetail != null ? minerDetail.getStandardName() : null)
                .price(companyMiner.getPrice())
                .currencyId(currency != null ? currency.getId() : null)
                .currencyCode(currency != null ? currency.getCode() : null)
                .currencySymbol(currency != null ? currency.getSymbol() : null)
                .priceConverted(companyMiner.getPriceConverted())
                .priceOld(companyMiner.getPriceOld())
                .hashrateMin(companyMiner.getHashrateMin())
                .hashrateMax(companyMiner.getHashrateMax())
                .hashrateUnitId(hashrateUnit != null ? hashrateUnit.getId() : null)
                .hashrateUnitAbbreviation(hashrateUnit != null ? hashrateUnit.getAbbreviation() : null)
                .quantity(companyMiner.getQuantity())
                .condition(companyMiner.getCondition())
                .active(companyMiner.getActive())
                .lowStockThreshold(companyMiner.getLowStockThreshold())
                .customFields(customFields)
                .createdAt(companyMiner.getCreatedAt())
                .updatedAt(companyMiner.getUpdatedAt())
                .build();
    }
}



