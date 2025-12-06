package com.miners.shop.dto;

import com.miners.shop.entity.Request;
import com.miners.shop.entity.Request.RequestStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * DTO для заявок
 * Используется для передачи данных между клиентом и сервером
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RequestDTO(
        Long id,
        Long offerId,
        Long whatsAppMessageId,
        String clientName,
        String clientPhone,
        String message,
        RequestStatus status,
        String adminComment,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt,
        // Информация о предложении
        OfferInfo offerInfo,
        // Информация о сообщении
        MessageInfo messageInfo,
        // Информация о продавце
        SellerInfo sellerInfo
) {
    
    /**
     * Информация о предложении
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OfferInfo(
            Long id,
            String productModel,
            String operationType,
            String price,
            String currency,
            Integer quantity,
            String condition,
            String location,
            String hashrate
    ) {}
    
    /**
     * Информация о сообщении WhatsApp
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MessageInfo(
            Long id,
            String chatName,
            String senderName,
            String senderPhone,
            String content,
            LocalDateTime timestamp
    ) {}
    
    /**
     * Информация о продавце
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SellerInfo(
            Long id,
            String name,
            String phone,
            String whatsappId,
            String contactInfo
    ) {}
    
    /**
     * Создать DTO из сущности
     */
    public static RequestDTO fromEntity(Request request) {
        if (request == null) {
            return null;
        }
        
        OfferInfo offerInfo = null;
        if (request.getOffer() != null) {
            var offer = request.getOffer();
            offerInfo = new OfferInfo(
                    offer.getId(),
                    offer.getProduct() != null ? offer.getProduct().getModel() : null,
                    offer.getOperationType() != null ? offer.getOperationType().name() : null,
                    offer.getPrice() != null ? offer.getPrice().toString() : null,
                    offer.getCurrency(),
                    offer.getQuantity(),
                    offer.getCondition(),
                    offer.getLocation(),
                    offer.getHashrate()
            );
        }
        
        MessageInfo messageInfo = null;
        if (request.getWhatsAppMessage() != null) {
            var msg = request.getWhatsAppMessage();
            messageInfo = new MessageInfo(
                    msg.getId(),
                    msg.getChatName(),
                    msg.getSenderName(),
                    msg.getSenderPhoneNumber(),
                    msg.getContent(),
                    msg.getTimestamp()
            );
        }
        
        SellerInfo sellerInfo = null;
        if (request.getOffer() != null && request.getOffer().getSeller() != null) {
            var seller = request.getOffer().getSeller();
            sellerInfo = new SellerInfo(
                    seller.getId(),
                    seller.getName(),
                    seller.getPhone(),
                    seller.getWhatsappId(),
                    seller.getContactInfo()
            );
        }
        
        return new RequestDTO(
                request.getId(),
                request.getOffer() != null ? request.getOffer().getId() : null,
                request.getWhatsAppMessage() != null ? request.getWhatsAppMessage().getId() : null,
                request.getClientName(),
                request.getClientPhone(),
                request.getMessage(),
                request.getStatus(),
                request.getAdminComment(),
                request.getCreatedAt(),
                request.getUpdatedAt(),
                offerInfo,
                messageInfo,
                sellerInfo
        );
    }
    
    /**
     * DTO для создания новой заявки
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CreateRequestDTO(
            Long offerId,
            String clientName,
            String clientPhone,
            String message,
            Boolean consentPersonalData
    ) {}
}


