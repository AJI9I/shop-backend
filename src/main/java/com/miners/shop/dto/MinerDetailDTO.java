package com.miners.shop.dto;

import com.miners.shop.entity.MinerDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO для детальной информации о майнере
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MinerDetailDTO {
    
    private Long id;
    private String standardName;
    private String manufacturer;
    private String series;
    private String hashrate;
    private String algorithm;
    private String powerConsumption;
    private String coins;
    private String powerSource;
    private String cooling;
    private String operatingTemperature;
    private String dimensions;
    private String noiseLevel;
    private String description;
    private String features;
    private String placementInfo;
    private String producerInfo;
    private String imageUrl;
    private Boolean active; // Флаг активности майнера
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Long> productIds; // ID товаров, связанных с этой детальной записью
    
    /**
     * Преобразует сущность в DTO
     */
    public static MinerDetailDTO fromEntity(MinerDetail minerDetail) {
        if (minerDetail == null) {
            return null;
        }
        
        MinerDetailDTO dto = MinerDetailDTO.builder()
                .id(minerDetail.getId())
                .standardName(minerDetail.getStandardName())
                .manufacturer(minerDetail.getManufacturer())
                .series(minerDetail.getSeries())
                .hashrate(minerDetail.getHashrate())
                .algorithm(minerDetail.getAlgorithm())
                .powerConsumption(minerDetail.getPowerConsumption())
                .coins(minerDetail.getCoins())
                .powerSource(minerDetail.getPowerSource())
                .cooling(minerDetail.getCooling())
                .operatingTemperature(minerDetail.getOperatingTemperature())
                .dimensions(minerDetail.getDimensions())
                .noiseLevel(minerDetail.getNoiseLevel())
                .description(minerDetail.getDescription())
                .features(minerDetail.getFeatures())
                .placementInfo(minerDetail.getPlacementInfo())
                .producerInfo(minerDetail.getProducerInfo())
                .imageUrl(minerDetail.getImageUrl())
                .active(minerDetail.getActive() != null ? minerDetail.getActive() : true)
                .createdAt(minerDetail.getCreatedAt())
                .updatedAt(minerDetail.getUpdatedAt())
                .build();
        
        // Добавляем ID связанных товаров
        if (minerDetail.getProducts() != null) {
            dto.setProductIds(minerDetail.getProducts().stream()
                    .map(product -> product.getId())
                    .collect(Collectors.toList()));
        }
        
        return dto;
    }
    
    /**
     * Преобразует DTO в сущность (для создания/обновления)
     */
    public MinerDetail toEntity() {
        MinerDetail minerDetail = new MinerDetail();
        minerDetail.setId(this.id);
        minerDetail.setStandardName(this.standardName);
        minerDetail.setManufacturer(this.manufacturer);
        minerDetail.setSeries(this.series);
        minerDetail.setHashrate(this.hashrate);
        minerDetail.setAlgorithm(this.algorithm);
        minerDetail.setPowerConsumption(this.powerConsumption);
        minerDetail.setCoins(this.coins);
        minerDetail.setPowerSource(this.powerSource);
        minerDetail.setCooling(this.cooling);
        minerDetail.setOperatingTemperature(this.operatingTemperature);
        minerDetail.setDimensions(this.dimensions);
        minerDetail.setNoiseLevel(this.noiseLevel);
        minerDetail.setDescription(this.description);
        minerDetail.setFeatures(this.features);
        minerDetail.setPlacementInfo(this.placementInfo);
        minerDetail.setProducerInfo(this.producerInfo);
        minerDetail.setImageUrl(this.imageUrl);
        minerDetail.setActive(this.active != null ? this.active : true);
        return minerDetail;
    }
}

