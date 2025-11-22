package com.miners.shop.service;

import com.miners.shop.entity.Seller;
import com.miners.shop.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с продавцами
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SellerService {
    
    private final SellerRepository sellerRepository;
    
    /**
     * Находит или создает продавца по телефону и имени
     * @param phone - Телефон продавца (уникальный идентификатор)
     * @param name - Имя продавца
     * @param whatsappId - ID из WhatsApp (опционально)
     * @return Существующий или новый продавец
     */
    @Transactional
    public Seller findOrCreateSeller(String phone, String name, String whatsappId) {
        if (phone == null || phone.isEmpty()) {
            log.warn("Телефон продавца не указан, невозможно создать/найти продавца");
            return null;
        }
        
        // Ищем существующего продавца по телефону
        Optional<Seller> existingSeller = sellerRepository.findByPhone(phone);
        
        if (existingSeller.isPresent()) {
            Seller seller = existingSeller.get();
            
            // Обновляем имя, если оно изменилось или было пустым
            if (name != null && !name.isEmpty() && 
                (seller.getName() == null || seller.getName().isEmpty() || !seller.getName().equals(name))) {
                log.debug("Обновление имени продавца {}: {} -> {}", phone, seller.getName(), name);
                seller.setName(name);
            }
            
            // Обновляем WhatsApp ID, если он не был установлен
            if (whatsappId != null && !whatsappId.isEmpty() && 
                (seller.getWhatsappId() == null || seller.getWhatsappId().isEmpty())) {
                seller.setWhatsappId(whatsappId);
            }
            
            sellerRepository.save(seller);
            log.debug("Найден существующий продавец: {} (ID: {})", phone, seller.getId());
            return seller;
        } else {
            // Создаем нового продавца
            Seller newSeller = new Seller();
            newSeller.setPhone(phone);
            newSeller.setName(name != null && !name.isEmpty() ? name : "Неизвестный продавец");
            newSeller.setWhatsappId(whatsappId);
            
            Seller saved = sellerRepository.save(newSeller);
            log.info("Создан новый продавец: {} (ID: {})", phone, saved.getId());
            return saved;
        }
    }
    
    /**
     * Получает продавца по ID
     */
    @Transactional(readOnly = true)
    public Optional<Seller> getSellerById(Long id) {
        return sellerRepository.findById(id);
    }
    
    /**
     * Получает продавца по телефону
     */
    @Transactional(readOnly = true)
    public Optional<Seller> getSellerByPhone(String phone) {
        return sellerRepository.findByPhone(phone);
    }
    
    /**
     * Получает всех продавцов с пагинацией
     */
    @Transactional(readOnly = true)
    public Page<Seller> getAllSellers(Pageable pageable) {
        return sellerRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    
    /**
     * Получает продавцов, отсортированных по количеству предложений
     */
    @Transactional(readOnly = true)
    public Page<Seller> getSellersByOffersCount(Pageable pageable) {
        return sellerRepository.findAllOrderByOffersCountDesc(pageable);
    }
    
    /**
     * Получает продавцов, у которых есть предложения
     */
    @Transactional(readOnly = true)
    public List<Seller> getSellersWithOffers() {
        return sellerRepository.findSellersWithOffers();
    }
    
    /**
     * Обновляет контактную информацию продавца
     */
    @Transactional
    public Seller updateSellerContactInfo(Long sellerId, String contactInfo) {
        Optional<Seller> sellerOpt = sellerRepository.findById(sellerId);
        if (sellerOpt.isPresent()) {
            Seller seller = sellerOpt.get();
            seller.setContactInfo(contactInfo);
            return sellerRepository.save(seller);
        }
        return null;
    }
}

