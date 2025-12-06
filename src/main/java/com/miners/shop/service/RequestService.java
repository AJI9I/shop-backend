package com.miners.shop.service;

import com.miners.shop.dto.RequestDTO;
import com.miners.shop.entity.CompanyMiner;
import com.miners.shop.entity.Offer;
import com.miners.shop.entity.Request;
import com.miners.shop.entity.Request.RequestStatus;
import com.miners.shop.entity.WhatsAppMessage;
import com.miners.shop.repository.CompanyMinerRepository;
import com.miners.shop.repository.OfferRepository;
import com.miners.shop.repository.RequestRepository;
import com.miners.shop.repository.WhatsAppMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис для работы с заявками
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RequestService {
    
    private final RequestRepository requestRepository;
    private final OfferRepository offerRepository;
    private final CompanyMinerRepository companyMinerRepository;
    private final WhatsAppMessageRepository whatsAppMessageRepository;
    
    /**
     * Создать новую заявку
     * Поддерживает создание заявки как для Offer, так и для CompanyMiner
     */
    @Transactional
    public Request createRequest(RequestDTO.CreateRequestDTO createDTO) {
        // Валидация: должен быть указан либо offerId, либо companyMinerId
        if (createDTO.offerId() == null && createDTO.companyMinerId() == null) {
            throw new IllegalArgumentException("Необходимо указать либо ID предложения (offerId), либо ID майнера компании (companyMinerId)");
        }
        
        if (createDTO.offerId() != null && createDTO.companyMinerId() != null) {
            throw new IllegalArgumentException("Нельзя указать одновременно offerId и companyMinerId. Укажите только один из них.");
        }
        
        Request request = new Request();
        WhatsAppMessage whatsAppMessage = null;
        
        // Обработка заявки для Offer
        if (createDTO.offerId() != null) {
            log.info("Создание новой заявки для предложения ID={}, клиент: {}", 
                    createDTO.offerId(), createDTO.clientName());
            
            // Находим предложение
            Offer offer = offerRepository.findById(createDTO.offerId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Предложение с ID=" + createDTO.offerId() + " не найдено"));
            
            // Пытаемся найти связанное сообщение WhatsApp по sourceMessageId
            if (offer.getSourceMessageId() != null) {
                whatsAppMessage = whatsAppMessageRepository.findByMessageId(offer.getSourceMessageId())
                        .orElse(null);
            }
            
            request.setOffer(offer);
            request.setCompanyMiner(null);
        }
        
        // Обработка заявки для CompanyMiner
        if (createDTO.companyMinerId() != null) {
            log.info("Создание новой заявки для майнера компании ID={}, клиент: {}", 
                    createDTO.companyMinerId(), createDTO.clientName());
            
            // Находим майнер компании
            CompanyMiner companyMiner = companyMinerRepository.findById(createDTO.companyMinerId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Майнер компании с ID=" + createDTO.companyMinerId() + " не найден"));
            
            // Проверяем, что майнер активен
            if (companyMiner.getActive() == null || !companyMiner.getActive()) {
                throw new IllegalArgumentException("Майнер компании неактивен и недоступен для заявок");
            }
            
            request.setOffer(null);
            request.setCompanyMiner(companyMiner);
        }
        
        // Устанавливаем общие поля
        request.setWhatsAppMessage(whatsAppMessage);
        request.setClientName(createDTO.clientName());
        request.setClientPhone(createDTO.clientPhone());
        request.setMessage(createDTO.message());
        request.setStatus(RequestStatus.NEW);
        
        Request savedRequest = requestRepository.save(request);
        log.info("Заявка создана: ID={}, тип={}", 
                savedRequest.getId(), 
                savedRequest.getOffer() != null ? "Offer" : "CompanyMiner");
        
        return savedRequest;
    }
    
    /**
     * Получить все заявки с пагинацией
     */
    @Transactional(readOnly = true)
    public Page<Request> getAllRequests(Pageable pageable) {
        return requestRepository.findAllWithDetails(pageable);
    }
    
    /**
     * Получить заявки по статусу с пагинацией
     */
    @Transactional(readOnly = true)
    public Page<Request> getRequestsByStatus(RequestStatus status, Pageable pageable) {
        return requestRepository.findByStatus(status, pageable);
    }
    
    /**
     * Получить заявку по ID с полной информацией
     */
    @Transactional(readOnly = true)
    public Request getRequestById(Long id) {
        Request request = requestRepository.findByIdWithDetails(id);
        if (request == null) {
            throw new IllegalArgumentException("Заявка с ID=" + id + " не найдена");
        }
        return request;
    }
    
    /**
     * Пометить заявку как просмотренную (изменить статус с NEW на PROCESSED)
     */
    @Transactional
    public Request markAsViewed(Long id) {
        log.info("Пометка заявки ID={} как просмотренной", id);
        
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Заявка с ID=" + id + " не найдена"));
        
        // Если заявка еще новая, меняем статус на "В обработке"
        if (request.getStatus() == RequestStatus.NEW) {
            request.setStatus(RequestStatus.PROCESSED);
            request = requestRepository.save(request);
            log.info("Статус заявки ID={} изменен на PROCESSED", id);
        }
        
        return request;
    }
    
    /**
     * Обновить статус заявки
     */
    @Transactional
    public Request updateRequestStatus(Long id, RequestStatus status, String adminComment) {
        log.info("Обновление статуса заявки ID={} на {}", id, status);
        
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Заявка с ID=" + id + " не найдена"));
        
        request.setStatus(status);
        if (adminComment != null) {
            request.setAdminComment(adminComment);
        }
        
        return requestRepository.save(request);
    }
    
    /**
     * Подсчитать количество заявок по статусам
     */
    @Transactional(readOnly = true)
    public long countByStatus(RequestStatus status) {
        return requestRepository.countByStatus(status);
    }
    
    /**
     * Получить статистику по заявкам
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Long> getRequestStatistics() {
        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        stats.put("NEW", countByStatus(RequestStatus.NEW));
        stats.put("PROCESSED", countByStatus(RequestStatus.PROCESSED));
        stats.put("CLOSED", countByStatus(RequestStatus.CLOSED));
        stats.put("CANCELLED", countByStatus(RequestStatus.CANCELLED));
        stats.put("TOTAL", requestRepository.count());
        return stats;
    }
}

