package com.miners.shop.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.miners.shop.entity.Offer;
import com.miners.shop.entity.OperationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO для представления предложения в JSON формате (без циклических ссылок)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Исключаем null поля из JSON
public class OfferDTO {
    
    private Long id;
    
    private OperationType operationType;
    private BigDecimal price;
    private String currency;
    private Integer quantity;
    private String condition;
    private String notes;
    private String location;
    private String manufacturer;
    private String hashrate;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    /**
     * Создает DTO из сущности Offer
     * Безопасное преобразование с обработкой всех возможных null значений
     */
    public static OfferDTO fromEntity(Offer offer) {
        if (offer == null) {
            return null;
        }
        
        OfferDTO dto = new OfferDTO();
        try {
            dto.setId(offer.getId());
            dto.setOperationType(offer.getOperationType());
            dto.setPrice(offer.getPrice());
            
            // Безопасная обработка строковых полей (защита от null)
            dto.setCurrency(offer.getCurrency() != null ? offer.getCurrency() : null);
            dto.setQuantity(offer.getQuantity());
            dto.setCondition(offer.getCondition() != null ? offer.getCondition() : null);
            dto.setNotes(offer.getNotes() != null ? offer.getNotes() : null);
            dto.setLocation(offer.getLocation() != null ? offer.getLocation() : null);
            dto.setManufacturer(offer.getManufacturer() != null ? offer.getManufacturer() : null);
            dto.setHashrate(offer.getHashrate() != null ? offer.getHashrate() : null);
            dto.setUpdatedAt(offer.getUpdatedAt());
            
            return dto;
        } catch (Exception e) {
            // Логируем ошибку, но не бросаем исключение, чтобы не прерывать обработку других записей
            throw new RuntimeException("Ошибка при преобразовании Offer в DTO (ID=" + offer.getId() + "): " + e.getMessage(), e);
        }
    }
}

